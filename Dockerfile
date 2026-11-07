FROM eclipse-temurin:11-jre

# 安装 tini 进程守护、时区以及 Python3 和微软语音支撑环境
RUN apt-get update \
    && apt-get install -y --no-install-recommends tini tzdata ca-certificates python3 python3-pip \
    && pip install --no-cache-dir --break-system-packages edge-tts \
    && rm -rf /var/lib/apt/lists/*

# 设置默认上海时区
ENV TZ=Asia/Shanghai

WORKDIR /app

# 拷贝包含可执行 JAR 包、微软 Edge TTS 语音服务与启动入口
COPY build/libs/reader-2.5.4.jar /app/bin/reader.jar
COPY server/bin/tts_server.py /app/bin/tts_server.py
COPY entrypoint.sh /app/entrypoint.sh

RUN chmod +x /app/entrypoint.sh /app/bin/tts_server.py

EXPOSE 8080

ENTRYPOINT ["/usr/bin/tini", "--", "/app/entrypoint.sh"]
