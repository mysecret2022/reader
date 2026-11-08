#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Microsoft Edge Neural TTS Service for Reader Web
High-fidelity streaming synthesis with Singleflight concurrency control,
TTL + LRU sliding-window disk cache cleanup, and exponential backoff retry.
"""

import asyncio
import hashlib
import json
import os
import sys
import time
from http.server import HTTPServer, BaseHTTPRequestHandler
from urllib.parse import parse_qs, urlparse

try:
    import edge_tts
except ImportError:
    edge_tts = None

DEFAULT_CACHE_DIR = os.path.abspath(
    os.environ.get("TTS_CACHE_DIR", os.path.join(os.getcwd(), "storage", "cache", "tts"))
)
os.makedirs(DEFAULT_CACHE_DIR, exist_ok=True)

MAX_CACHE_SIZE_MB = int(os.environ.get("TTS_MAX_CACHE_MB", "200"))
TTL_SECONDS = int(os.environ.get("TTS_CACHE_TTL", str(24 * 3600)))  # 24 hours

# Singleflight in-flight map: hash -> asyncio.Task / Future
in_flight_tasks = {}
request_counter = 0

def maybe_clean_cache():
    """低频抽样清理 (每 50 次请求执行一次)，防止高频 stat() 造成磁盘 I/O 抖动"""
    global request_counter
    request_counter += 1
    if request_counter % 50 == 0:
        clean_expired_and_lru_cache()

VOICES = [
    {"id": "zh-CN-XiaoxiaoNeural", "name": "晓晓 (女声·小说优选)", "gender": "Female", "locale": "zh-CN"},
    {"id": "zh-CN-YunxiNeural", "name": "云希 (男声·生动多情)", "gender": "Male", "locale": "zh-CN"},
    {"id": "zh-CN-YunjianNeural", "name": "云健 (男声·热血沉稳)", "gender": "Male", "locale": "zh-CN"},
    {"id": "zh-CN-XiaoyiNeural", "name": "晓伊 (女声·灵动甜美)", "gender": "Female", "locale": "zh-CN"},
    {"id": "zh-CN-YunyangNeural", "name": "云扬 (男声·专业播音)", "gender": "Male", "locale": "zh-CN"},
    {"id": "zh-CN-liaoning-XiaobeiNeural", "name": "晓北 (东北女声·幽默)", "gender": "Female", "locale": "zh-CN"},
    {"id": "zh-HK-HiuMaanNeural", "name": "晓曼 (粤语女声·标准)", "gender": "Female", "locale": "zh-HK"},
    {"id": "zh-TW-HsiaoChenNeural", "name": "晓臻 (国语女声·台湾)", "gender": "Female", "locale": "zh-TW"},
]

def format_rate_pitch(rate: float, pitch: float):
    if rate > 1.0:
        rate_percent = int(round((rate - 1.0) * 100))
        rate_str = f"+{rate_percent}%"
    elif rate < 1.0 and rate > 0:
        rate_percent = int(round((1.0 - rate) * 100))
        rate_str = f"-{rate_percent}%"
    else:
        rate_str = "+0%"

    if pitch > 1.0:
        pitch_val = int(round((pitch - 1.0) * 50))
        pitch_str = f"+{pitch_val}Hz"
    elif pitch < 1.0 and pitch > 0:
        pitch_val = int(round((1.0 - pitch) * 50))
        pitch_str = f"-{pitch_val}Hz"
    else:
        pitch_str = "+0Hz"
    return rate_str, pitch_str

def clean_expired_and_lru_cache():
    """Triggered periodically or after generation: removes TTL expired files & trims to LRU quota"""
    try:
        now = time.time()
        files = []
        total_size = 0
        for f in os.listdir(DEFAULT_CACHE_DIR):
            fp = os.path.join(DEFAULT_CACHE_DIR, f)
            if os.path.isfile(fp) and f.endswith(".mp3"):
                stat = os.stat(fp)
                if now - stat.st_mtime > TTL_SECONDS:
                    try:
                        os.remove(fp)
                    except OSError:
                        pass
                    continue
                files.append((stat.st_mtime, stat.st_size, fp))
                total_size += stat.st_size

        max_bytes = MAX_CACHE_SIZE_MB * 1024 * 1024
        if total_size > max_bytes:
            files.sort(key=lambda x: x[0])  # oldest access/modified time first
            for mtime, size, fp in files:
                if total_size <= int(max_bytes * 0.8):
                    break
                try:
                    os.remove(fp)
                    total_size -= size
                except OSError:
                    pass
    except Exception as e:
        sys.stderr.write(f"[TTS Cache Clean Error]: {e}\n")

async def do_synthesize_with_retry(text: str, voice: str, rate_str: str, pitch_str: str, output_path: str, max_retries: int = 2):
    if not edge_tts:
        raise RuntimeError("edge-tts library is not installed.")

    delay = 0.5
    last_err = None
    tmp_path = output_path + f".{os.getpid()}.{time.time()}.tmp"

    for attempt in range(max_retries + 1):
        try:
            communicate = edge_tts.Communicate(text, voice, rate=rate_str, pitch=pitch_str)
            await communicate.save(tmp_path)
            if os.path.exists(tmp_path) and os.path.getsize(tmp_path) > 0:
                os.replace(tmp_path, output_path)
                return True
            raise RuntimeError("Generated empty audio stream")
        except Exception as e:
            last_err = e
            if os.path.exists(tmp_path):
                try:
                    os.remove(tmp_path)
                except OSError:
                    pass
            if attempt < max_retries:
                await asyncio.sleep(delay)
                delay *= 2
    if last_err:
        raise last_err
    return False

# Global event loop for thread safety
GLOBAL_LOOP = None

def get_event_loop():
    global GLOBAL_LOOP
    if GLOBAL_LOOP is None or GLOBAL_LOOP.is_closed():
        try:
            GLOBAL_LOOP = asyncio.get_event_loop()
        except RuntimeError:
            GLOBAL_LOOP = asyncio.new_event_loop()
            asyncio.set_event_loop(GLOBAL_LOOP)
    return GLOBAL_LOOP


class TTSRequestHandler(BaseHTTPRequestHandler):
    def end_headers(self):
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "*")
        super().end_headers()

    def do_OPTIONS(self):
        self.send_response(200)
        self.end_headers()

    def _serve_synthesize(self, text, voice, rate, pitch):
        if not text:
            self.send_response(400)
            self.send_header("Content-Type", "text/plain; charset=utf-8")
            self.end_headers()
            self.wfile.write(b"Missing text parameter")
            return

        rate_str, pitch_str = format_rate_pitch(rate, pitch)
        cache_key = f"{text}|{voice}|{rate_str}|{pitch_str}"
        sha256_hash = hashlib.sha256(cache_key.encode("utf-8")).hexdigest()
        cache_file = os.path.join(DEFAULT_CACHE_DIR, f"{sha256_hash}.mp3")

        # 1. Cache HIT
        if os.path.exists(cache_file) and os.path.getsize(cache_file) > 0:
            try:
                os.utime(cache_file, None)  # update access time for LRU
            except OSError:
                pass
            self._send_audio_file(cache_file, "HIT")
            return

        # 2. Singleflight handling
        loop = get_event_loop()
        is_initiator = False

        if sha256_hash not in in_flight_tasks:
            is_initiator = True
            fut = loop.create_future()
            in_flight_tasks[sha256_hash] = fut

        if not is_initiator:
            fut = in_flight_tasks[sha256_hash]
            try:
                loop.run_until_complete(fut)
            except Exception as e:
                self.send_response(500)
                self.send_header("Content-Type", "text/plain; charset=utf-8")
                self.end_headers()
                self.wfile.write(f"TTS synthesis error: {e}".encode("utf-8"))
                return

            if os.path.exists(cache_file) and os.path.getsize(cache_file) > 0:
                self._send_audio_file(cache_file, "SINGLEFLIGHT")
                return

        # Initiator runs the actual Edge-TTS fetch
        try:
            loop.run_until_complete(
                do_synthesize_with_retry(text, voice, rate_str, pitch_str, cache_file, max_retries=2)
            )
            maybe_clean_cache()
            if not fut.done():
                fut.set_result(True)
        except Exception as e:
            if not fut.done():
                fut.set_exception(e)
            self.send_response(500)
            self.send_header("Content-Type", "text/plain; charset=utf-8")
            self.end_headers()
            self.wfile.write(f"TTS synthesis error: {e}".encode("utf-8"))
            return
        finally:
            in_flight_tasks.pop(sha256_hash, None)

        if os.path.exists(cache_file) and os.path.getsize(cache_file) > 0:
            self._send_audio_file(cache_file, "MISS")
        else:
            self.send_response(500)
            self.send_header("Content-Type", "text/plain; charset=utf-8")
            self.end_headers()
            self.wfile.write(b"Audio generation failed")

    def _send_audio_file(self, filepath, cache_status):
        try:
            # 显式更新 mtime 为当前时间，在 Linux/Docker (relatime/noatime) 环境下保证 LRU 准确性
            os.utime(filepath, None)
        except OSError:
            pass
        file_size = os.path.getsize(filepath)
        self.send_response(200)
        self.send_header("Content-Type", "audio/mpeg")
        self.send_header("Content-Length", str(file_size))
        self.send_header("X-Cache-Lookup", cache_status)
        self.send_header("Cache-Control", "public, max-age=604800")
        self.end_headers()
        with open(filepath, "rb") as f:
            while chunk := f.read(65536):
                self.wfile.write(chunk)

    def do_POST(self):
        parsed = urlparse(self.path)
        path = parsed.path
        if path in ("/tts", "/reader3/tts", "/api/tts/synthesize"):
            try:
                content_length = int(self.headers.get("Content-Length", 0))
                body = self.rfile.read(content_length)
                data = json.loads(body.decode("utf-8"))
            except Exception:
                data = {}

            text = data.get("text", "").strip()
            voice = data.get("voice", "zh-CN-XiaoxiaoNeural").strip()
            rate = float(data.get("rate", 1.0))
            pitch = float(data.get("pitch", 1.0))
            self._serve_synthesize(text, voice, rate, pitch)
            return

        self.send_response(404)
        self.end_headers()

    def do_GET(self):
        parsed = urlparse(self.path)
        path = parsed.path

        if path in ("/health", "/reader3/tts/health"):
            self.send_response(200)
            self.send_header("Content-Type", "application/json; charset=utf-8")
            self.end_headers()
            self.wfile.write(json.dumps({"status": "ok", "service": "edge-tts"}).encode("utf-8"))
            return

        if path in ("/voices", "/reader3/tts/voices"):
            self.send_response(200)
            self.send_header("Content-Type", "application/json; charset=utf-8")
            self.end_headers()
            self.wfile.write(json.dumps(VOICES, ensure_ascii=False).encode("utf-8"))
            return

        if path in ("/tts", "/reader3/tts", "/api/tts/synthesize"):
            query = parse_qs(parsed.query)
            text = query.get("text", [""])[0].strip()
            voice = query.get("voice", ["zh-CN-XiaoxiaoNeural"])[0].strip()
            try:
                rate = float(query.get("rate", ["1.0"])[0])
            except ValueError:
                rate = 1.0
            try:
                pitch = float(query.get("pitch", ["1.0"])[0])
            except ValueError:
                pitch = 1.0

            self._serve_synthesize(text, voice, rate, pitch)
            return

        self.send_response(404)
        self.end_headers()

    def log_message(self, format, *args):
        pass


def main():
    port = int(os.environ.get("TTS_PORT", 8089))
    host = os.environ.get("TTS_HOST", "127.0.0.1")
    server = HTTPServer((host, port), TTSRequestHandler)
    print(f"Edge TTS service listening on http://{host}:{port}")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        server.server_close()


if __name__ == "__main__":
    main()
