FROM eclipse-temurin:26
RUN apt-get update && \
    apt-get install -y \
      ffmpeg \
      vainfo \
      libva2 \
      i965-va-driver \
      intel-media-va-driver \
      va-driver-all && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY target/coffee-house-1.0.0.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
