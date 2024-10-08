# Forex-mtl

This project is a live interpreter for a one-frame service that provides exchange rate information. The application operates under the following logic:

1. The service returns the exchange rate when two supported currencies are provided.
2. The exchange rate must not be older than 5 minutes.
3. The service is designed to handle at least 10,000 successful requests per day with a single API token.

Please note that the one-service API has a limitation of 1,000 calls per day.

## Prerequisite
- Docker
- Java ~ openjdk 17.0.12 2024-07-16
- scalaVersion "2.13.12"
- sbt.version "1.9.2"

## How to Run This Project Locally
please copy .env.example to .env and filled the values with the correct value before Run the project
for ONE_FRAME_TOKEN you can use: `10dc303535874aeccc86a8251e6992f5` like mentioned on https://hub.docker.com/r/paidyinc/one-frame
there is some option that you can do: 
1. Using docker
   - Start all in once by running `docker-compose --profile app up -d --build`.
2. Compile and run the project locally:
    - `docker-compose up -d` --> spawn Dependencies on local
    - `make all` -> to execute sbt clean,update n compile.
    - `make run` -> to run the app.
3. Run test only
   - Execute tests via the SBT console.
   OR
   - `make test`
please copy .env.example to .env and filled the values with the correct value before Run the project 


# Detailed Solution
## System Diagram
![local-proxy.drawio.png](local-proxy.drawio.png)
The following diagram illustrates the workflow for the /rates API endpoint. It highlights the different components and logic involved in processing requests and managing metrics.


Workflow Description
Middleware (Logging & Metrics):

Every request to the /rates endpoint goes through middleware that handles logging and metrics collection. This ensures that relevant data is captured for monitoring and troubleshooting.
Rates Handler:

The /rates handler receives the incoming request. At this point, it checks if the requested currency pair is supported.
Supported Pair Check:

The system verifies whether the currency pair specified in the request is supported. If the pair is not supported, the handler will return an error response.
Call Wrapper:

If the pair is supported, the request is processed using the callWrapper class. This component is responsible for managing the logic related to caching and circuit breaking:
Cached TTL Check: The callWrapper checks if the response for the requested currency pair is cached and whether it is still valid (TTL < 5 minutes).
Circuit Breaker State: If the cached data is valid:
Closed State: The request proceeds to call the OneFrameAPI to fetch the latest data.
Open State: If the circuit breaker is in an open state, an error response is returned, indicating that the service is temporarily unavailable.
Push Metric:

If the cached data is still valid and used, the system logs the metrics using the PushMetric method, ensuring that performance data is recorded for monitoring purposes.
Response:

Finally, the processed response is sent back to the client, either with the fetched data from OneFrameAPI or with an error message if the currency pair is not supported or if there was an issue during processing.
Conclusion
This workflow ensures that the /rates API endpoint operates efficiently by leveraging caching and circuit breaking, while also collecting valuable metrics for observability. This architecture allows for quick responses and robust error handling in the case of service disruptions.

# Technical Overview
In this project, I tried to implement these technical aspects to achieve a good microservice standard.
## Frameworks and Libraries
- **HTTP Framework**: http4s
- **Configuration Management**: PureConfig
- **Circuit Breaker and Resilience**: Utilize Resilience4j Circuit Breaker patterns to ensure resilient communication between services and gracefully handle failures.
- **Asynchronous and Non-blocking IO**: Leverage Scala's functional programming ecosystem, utilizing libraries such as Cats Effect, ZIO, or Monix for effective concurrency management, non-blocking I/O, and error handling.

## Resilience and Fault Tolerance
- Implement the Circuit Breaker pattern to prevent cascading failures across services.
- ~~Introduce the SingleFlight mechanism to optimize redundant requests.~~ The SingleFlight mechanism could not be implemented due to dependency version conflicts. This feature may be added in the future when compatibility issues are resolved.

## Testing
- **Unit Testing**: Employ ScalaTest or MUnit for unit testing individual functions and logic.

## Observability (Logging, Monitoring, and Tracing)
- **Structured Logging**: Use Logback to implement structured logging, ideally in JSON format.
- **Metrics and Monitoring**: Integrate with Prometheus for metrics collection and Grafana for monitoring dashboards. Utilize libraries like Micrometer to export JVM metrics (memory, CPU usage, thread pools).
- ~~**Distributed Tracing**: Employ tracing libraries such as OpenTelemetry to monitor requests across distributed services, aiding in performance tracking and troubleshooting.~~ To Be Planned

## Caching
- **Caching**: Implement Memcached to cache frequently accessed data, improving response times and reducing database load.

## Security
- **Data Validation**: Ensure robust data validation.
- **HTTPS**: Ensure secure communication with other microservices in a production environment. This possible by set the host value on ENV params. 

## Continuous Integration and Continuous Deployment (CI/CD)
- Establish CI/CD pipelines using tools like GitHub Actions to automate testing, building, and deployment within this microservice's architecture.
