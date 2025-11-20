# Dockerfile
FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

# 复制 JAR 文件
COPY target/scala-3.*/nexus-assembly-*.jar /app/app.jar

# 创建非 root 用户
RUN addgroup -g 1000 nexus && \
    adduser -u 1000 -G nexus -s /bin/sh -D nexus && \
    chown -R nexus:nexus /app

USER nexus

# 默认运行主应用
ENTRYPOINT ["java"]
CMD ["-jar", "app.jar", "server"]

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/health || exit 1