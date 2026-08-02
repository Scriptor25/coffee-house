FROM eclipse-temurin:26
RUN apt-get update && apt-get install -y ffmpeg

WORKDIR /app

COPY target/coffee-house-1.0.0.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
