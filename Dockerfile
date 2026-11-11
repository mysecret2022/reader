FROM eclipse-temurin:11-jre-jammy

# 安装 tini 进程守护、时区与 CA 证书
RUN apt-get update \
    && apt-get install -y --no-install-recommends tini tzdata ca-certificates \
    && rm -rf /var/lib/apt/lists/*

# 设置默认上海时区与默认 JVM 内存控制参数（初始 128M，最大 350M，启用 G1 垃圾回收与字符串去重）
ENV TZ=Asia/Shanghai \
    JAVA_OPTS="-Xms128m -Xmx350m -XX:+UseG1GC -XX:+UseStringDeduplication"

WORKDIR /app

# 拷贝可执行 JAR 包与启动入口
COPY build/libs/reader-*.jar /app/bin/reader.jar
COPY entrypoint.sh /app/entrypoint.sh

RUN chmod +x /app/entrypoint.sh

EXPOSE 8080

ENTRYPOINT ["/usr/bin/tini", "--", "/app/entrypoint.sh"]
