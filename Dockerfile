FROM mcr.microsoft.com/openjdk/jdk:21-ubuntu
WORKDIR /app
COPY build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"] 