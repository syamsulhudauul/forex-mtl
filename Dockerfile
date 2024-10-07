FROM sbtscala/scala-sbt:openjdk-8u342_1.8.0_2.13.10 as builder

WORKDIR /app
COPY . /app
RUN sbt assembly

FROM openjdk:8-jre-alpine

WORKDIR /app
COPY --from=builder /app/target/scala-2.13/*-assembly-*.jar /app/app.jar

EXPOSE 8080

CMD ["java", "-jar", "/app/app.jar"]