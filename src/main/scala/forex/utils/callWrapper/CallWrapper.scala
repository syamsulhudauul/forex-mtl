package forex.utils.callWrapper

import cats.effect.Sync
import io.github.resilience4j.circuitbreaker.{CallNotPermittedException, CircuitBreaker, CircuitBreakerConfig}

import java.time.{Duration, Instant}
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import org.slf4j.LoggerFactory
import forex.config._
import cats.effect.concurrent.Ref
import cats.syntax.functor._

// CallWrapper is a general package to ease integration with other services.
// Requires defining CallWrapperConfig to use this class.
// It can potentially be used to cover more usages like:
// * SingleFlight
// * Standardize Metric utilization
class CallWrapper[F[_]: Sync](cfg: CallWrapperConfig) {
  private val logger = LoggerFactory.getLogger(getClass)
  // Circuit breaker configuration
  require(cfg.failureRateThreshold >= 0 && cfg.failureRateThreshold <= 100, "failureRateThreshold must be between 0 and 100")
  require(cfg.waitDurationInOpenState.toSeconds > 0, "waitDurationInOpenState must be positive")
  private val cbConfig = CircuitBreakerConfig.custom()
    .failureRateThreshold(cfg.failureRateThreshold.toFloat) // Use the parameter
    .waitDurationInOpenState(Duration.ofSeconds(cfg.waitDurationInOpenState.toSeconds)) // Use the parameter
    .build()

  // Create the circuit breaker instance
  private val circuitBreaker: Option[CircuitBreaker] =
    if (cfg.enableCb) Some(CircuitBreaker.of(cfg.cbName, cbConfig)) else None

  // In-memory cache
  private val cache: Ref[F, Map[String, (Instant, Any)]] = Ref.unsafe(Map.empty[String, (Instant, Any)])

  // Function to execute an action with circuit breaker protection
  def call[A](key: String)(action: F[A]): F[A] = {
    if (cfg.enableCache) {
      for {
        currentCache <- cache.get
        result <- currentCache.get(key) match {
          case Some((timestamp, value)) if Instant.now().isBefore(timestamp.plusSeconds(cfg.cacheTTL.toSeconds)) =>
            // Return cached value if TTL is valid
            Sync[F].pure(value.asInstanceOf[A])
          case _ =>
            // Remove expired cache entry and execute the action
            executeWithCircuitBreaker(action).flatTap { result =>
              // Cache the result with the current timestamp
              cache.update(_ + (key -> (Instant.now(), result)))
            }
        }
      } yield result
    }else {
        // If caching is disabled, just execute the action with optional circuit breaker protection
        executeWithCircuitBreaker(action)
      }
    }

  // Execute action with optional circuit breaker protection
  private def executeWithCircuitBreaker[A](action: F[A]): F[A] = {
    circuitBreaker match {
      case Some(cb) =>
        // Execute with circuit breaker
        Sync[F].delay(cb.executeSupplier(() => action)).flatten.handleErrorWith(handleCircuitBreakerError)
      case None =>
        // Execute without circuit breaker
        action
    }
  }

  // Handle errors related to circuit breaker behavior (e.g., when calls are not permitted)
  private def handleCircuitBreakerError[A](error: Throwable): F[A] = error match {
    case _: CallNotPermittedException =>
      logger.warn("Circuit breaker: Call not permitted")
      Sync[F].raiseError(new RuntimeException("Circuit breaker: Call not permitted. Service is unavailable."))
    case other =>
      logger.error(s"Unexpected error: ${other.getMessage}")
      Sync[F].raiseError(new RuntimeException(s"Unexpected error: ${other.getMessage}"))
  }
}
