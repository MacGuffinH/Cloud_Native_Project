# 使用 Java 作为基础镜像
FROM openjdk:11-jdk-slim as build

# 将当前目录添加到容器的 /app 目录
WORKDIR /app
COPY . /app

# 使用 Maven 构建项目
RUN ./mvnw clean install

# 创建运行镜像
FROM openjdk:11-jre-slim
COPY --from=build /app/target/your-app.jar /app/your-app.jar

# 暴露应用端口
EXPOSE 8080
CMD ["java", "-jar", "/app/your-app.jar"]
