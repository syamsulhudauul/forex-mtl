FROM openjdk:11
WORKDIR /app
COPY ./target/scala-2.13/my-service.jar .
ENTRYPOINT ["java", "-jar", "my-service.jar"]
