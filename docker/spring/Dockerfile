FROM openjdk:11-jdk-slim
ARG JAR_FILE=target
ARG APP_NAME=app
ARG PORT=port
COPY ${JAR_FILE} app.jar

ENTRYPOINT ["java", "-jar", "/app.jar", "-XX:+UseContainerSupport", "-XshowSettings:vm"]

EXPOSE ${PORT}
