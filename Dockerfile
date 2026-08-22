FROM debian:trixie-slim

RUN apt-get update && apt-get install -y --no-install-recommends \
    openjdk-25-jre \
    ffmpeg \
    libva2 \
    va-driver-all \
    i965-va-driver \
    intel-media-va-driver \
    locales \
    && rm -rf /var/lib/apt/lists/*

ENV LANG=C.UTF-8
ENV LC_ALL=C.UTF-8

WORKDIR /app

COPY target/coffee-house-1.0.0.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
