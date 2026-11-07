FROM eclipse-temurin:11-jre-alpine

# 安装 tini 进程守护与基础时区支持
RUN apk add --no-cache ca-certificates tini tzdata \
    && update-ca-certificates \
    && rm -rf /var/cache/apk/*

# 设置默认上海时区
ENV TZ=Asia/Shanghai

WORKDIR /app

# 拷贝包含全新高颜值免弹窗登录界面的可执行 JAR 包
COPY build/libs/reader-2.5.4.jar /app/bin/reader.jar

EXPOSE 8080

ENTRYPOINT ["/sbin/tini", "--"]
CMD ["java", "-jar", "/app/bin/reader.jar"]
