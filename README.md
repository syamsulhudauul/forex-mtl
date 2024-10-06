# Forex-mtl

This project is a live interpreter for a one-frame service that provides exchange rate information. The application operates under the following logic:

1. The service returns the exchange rate when two supported currencies are provided.
2. The exchange rate must not be older than 5 minutes.
3. The service is designed to handle at least 10,000 successful requests per day with a single API token.

Please note that the one-service API has a limitation of 1,000 calls per day.

## How to Run This Project Locally
1. Start the One Frame service by running `docker-compose up -d`.
2. Compile and run the project:
    - Enter the SBT console.
    - Execute the `compile` command.
    - Run the application.
3. Execute tests via the SBT console.

# Detailed Solution
TBA

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
- **Coverage**: TBA %

## Observability (Logging, Monitoring, and Tracing)
- **Structured Logging**: Use Logback to implement structured logging, ideally in JSON format.
- **Metrics and Monitoring**: Integrate with Prometheus for metrics collection and Grafana for monitoring dashboards. Utilize libraries like Micrometer to export JVM metrics (memory, CPU usage, thread pools).
- **Distributed Tracing**: Employ tracing libraries such as OpenTelemetry or Zipkin to monitor requests across distributed services, aiding in performance tracking and troubleshooting.

## Caching
- **Caching**: Implement Memcached to cache frequently accessed data, improving response times and reducing database load.

## Security
- **Data Validation**: Ensure robust data validation.
- **HTTPS**: Ensure secure communication with other microservices in a production environment.

## Continuous Integration and Continuous Deployment (CI/CD)
- Establish CI/CD pipelines using tools like GitHub Actions to automate testing, building, and deployment within this microservice's architecture.
