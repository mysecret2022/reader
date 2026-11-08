#!/bin/sh
set -e

# 兼容根路径挂载 (/storage 与 /logs)
if [ -d /storage ] && [ ! -L /app/storage ]; then
    rm -rf /app/storage
    ln -s /storage /app/storage
fi
if [ -d /logs ] && [ ! -L /app/logs ]; then
    rm -rf /app/logs
    ln -s /logs /app/logs
fi

# 启动 Edge TTS 微软神经语音后台微服务（若存在）
if [ -f /app/bin/tts_server.py ]; then
    echo "Starting Microsoft Edge TTS microservice..."
    python3 /app/bin/tts_server.py &
fi

# 启动阅读主服务
exec java -jar /app/bin/reader.jar "$@"
