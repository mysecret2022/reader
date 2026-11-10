#!/bin/sh
set -e

# 兼容根路径挂载与 /app 路径挂载 (/storage 与 /app/storage)
if [ -d /storage ] && [ ! -L /app/storage ]; then
    rm -rf /app/storage
    ln -s /storage /app/storage
elif [ -d /app/storage ] && [ ! -d /storage ] && [ ! -L /storage ]; then
    ln -s /app/storage /storage
fi

# 兼容日志目录双向挂载 (/logs 与 /app/logs)
if [ -d /app/logs ] && [ ! -d /logs ] && [ ! -L /logs ]; then
    ln -s /app/logs /logs
elif [ -d /logs ] && [ ! -L /app/logs ]; then
    rm -rf /app/logs
    ln -s /logs /app/logs
fi
mkdir -p /app/logs /logs 2>/dev/null || true

# 默认 JVM 内存控制参数（初始堆 128M，最大堆 350M，启用 G1GC 与字符串去重）
# 支持通过环境变量 JAVA_OPTS 灵活覆盖
JAVA_OPTS="${JAVA_OPTS:--Xms128m -Xmx350m -XX:+UseG1GC -XX:+UseStringDeduplication}"

echo "Starting Reader with JAVA_OPTS: ${JAVA_OPTS}"
exec java $JAVA_OPTS -jar /app/bin/reader.jar "$@"
