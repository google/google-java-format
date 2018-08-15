FROM openjdk:alpine

RUN mkdir -p /app
WORKDIR /app

ADD https://github.com/google/google-java-format/releases/download/google-java-format-1.6/google-java-format-1.6-all-deps.jar /app/google-java-format.jar

ENTRYPOINT ["/usr/bin/java", "-jar", "/app/google-java-format.jar"]
