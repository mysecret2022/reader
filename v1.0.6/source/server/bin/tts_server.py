#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Microsoft Edge Neural TTS Service for Reader Web
Provides high-fidelity, natural neural speech synthesis with local caching.
"""

import asyncio
import hashlib
import json
import os
import sys
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

async def do_synthesize(text: str, voice: str, rate: float, pitch: float, output_path: str):
    if not edge_tts:
        raise RuntimeError("edge-tts library is not installed.")
    
    # Rate: 0.5 ~ 2.0 -> percentage string e.g. -20% or +20%
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

    communicate = edge_tts.Communicate(text, voice, rate=rate_str, pitch=pitch_str)
    await communicate.save(output_path)


class TTSRequestHandler(BaseHTTPRequestHandler):
    def end_headers(self):
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "*")
        super().end_headers()

    def do_OPTIONS(self):
        self.send_response(200)
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

        if path in ("/tts", "/reader3/tts"):
            query = parse_qs(parsed.query)
            text = query.get("text", [""])[0].strip()
            voice = query.get("voice", ["zh-CN-XiaoxiaoNeural"])[0].strip()
            rate = float(query.get("rate", ["1.0"])[0])
            pitch = float(query.get("pitch", ["1.0"])[0])

            if not text:
                self.send_response(400)
                self.send_header("Content-Type", "text/plain; charset=utf-8")
                self.end_headers()
                self.wfile.write(b"Missing text parameter")
                return

            # Check cache
            cache_key = f"{text}|{voice}|{rate}|{pitch}"
            md5_hash = hashlib.md5(cache_key.encode("utf-8")).hexdigest()
            cache_file = os.path.join(DEFAULT_CACHE_DIR, f"{md5_hash}.mp3")

            if not os.path.exists(cache_file) or os.path.getsize(cache_file) == 0:
                tmp_file = cache_file + ".tmp"
                try:
                    asyncio.run(do_synthesize(text, voice, rate, pitch, tmp_file))
                    if os.path.exists(tmp_file) and os.path.getsize(tmp_file) > 0:
                        os.replace(tmp_file, cache_file)
                    else:
                        raise RuntimeError("Empty audio generated")
                except Exception as e:
                    if os.path.exists(tmp_file):
                        try:
                            os.remove(tmp_file)
                        except OSError:
                            pass
                    self.send_response(500)
                    self.send_header("Content-Type", "text/plain; charset=utf-8")
                    self.end_headers()
                    self.wfile.write(f"TTS synthesis error: {e}".encode("utf-8"))
                    return

            if os.path.exists(cache_file):
                file_size = os.path.getsize(cache_file)
                self.send_response(200)
                self.send_header("Content-Type", "audio/mpeg")
                self.send_header("Content-Length", str(file_size))
                self.send_header("Cache-Control", "public, max-age=604800")
                self.end_headers()
                with open(cache_file, "rb") as f:
                    while chunk := f.read(65536):
                        self.wfile.write(chunk)
                return

        self.send_response(404)
        self.end_headers()

    def log_message(self, format, *args):
        # Silence verbose logging
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
