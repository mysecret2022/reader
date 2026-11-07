FROM eclipse-temurin:11-jre

# 安装 tini 进程守护与基础时区支持
RUN apt-get update \
    && apt-get install -y --no-install-recommends tini tzdata ca-certificates \
    && rm -rf /var/lib/apt/lists/*

# 设置默认上海时区
ENV TZ=Asia/Shanghai

WORKDIR /app

# 拷贝包含全新高颜值免弹窗登录界面的可执行 JAR 包
COPY build/libs/reader-2.5.4.jar /app/bin/reader.jar

EXPOSE 8080

ENTRYPOINT ["/usr/bin/tini", "--"]
CMD ["java", "-jar", "/app/bin/reader.jar"]
