FROM eclipse-temurin:26

WORKDIR /app

COPY target/coffee-house-1.0.0.jar app.jar

ENV HOSTNAME=0.0.0.0
ENV PORT=8080

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
