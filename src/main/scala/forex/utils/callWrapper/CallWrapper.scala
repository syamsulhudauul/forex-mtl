package forex.utils.callWrapper

import cats.effect.Sync
import io.github.resilience4j.circuitbreaker.{CallNotPermittedException, CircuitBreaker, CircuitBreakerConfig}
import java.time.{Duration => JavaDuration, Instant}
import cats.syntax.all._
import org.slf4j.LoggerFactory
import forex.config._
import cats.effect.concurrent.Ref
import forex.utils.metrics.Metrics
import scala.concurrent.duration.{Duration => ScalaDuration}

class CallWrapper[F[_]: Sync](cfg: CallWrapperConfig, metrics: Metrics) {
  private val logger = LoggerFactory.getLogger(getClass)

  require(cfg.failureRateThreshold >= 0 && cfg.failureRateThreshold <= 100, "failureRateThreshold must be between 0 and 100")
  require(cfg.waitDurationInOpenState.toSeconds > 0, "waitDurationInOpenState must be positive")
  private val cbConfig = CircuitBreakerConfig.custom()
    .failureRateThreshold(cfg.failureRateThreshold.toFloat)
    .waitDurationInOpenState(JavaDuration.ofSeconds(cfg.waitDurationInOpenState.toSeconds))
    .build()

  private val circuitBreaker: Option[CircuitBreaker] =
    if (cfg.enableCb) Some(CircuitBreaker.of(s"${cfg.name}_cb", cbConfig)) else None

  private val cache: Ref[F, Map[String, (Instant, Any)]] = Ref.unsafe(Map.empty[String, (Instant, Any)])

  def call[A](key: String)(action: F[A]): F[A] = {
    val startTime = System.nanoTime()

    val result = if (cfg.enableCache) {
      for {
        currentCache <- cache.get
        result <- currentCache.get(key) match {
          case Some((timestamp, value)) if Instant.now().isBefore(timestamp.plusSeconds(cfg.cacheTTL.toSeconds)) =>
            Sync[F].pure(value.asInstanceOf[A])
          case _ =>
            executeWithCircuitBreaker(action).flatTap { result =>
              cache.update(_ + (key -> (Instant.now(), result)))
            }
        }
      } yield result
    } else {
      executeWithCircuitBreaker(action)
    }

    result.flatTap { _ =>
      Sync[F].delay {
        val duration = ScalaDuration.fromNanos(System.nanoTime() - startTime)
        val tags = Map(
          "name" -> cfg.name,
          "cached" -> cfg.enableCache.toString,
          "circuit_breaker" -> cfg.enableCb.toString,
          "result" -> "success"
        )
        metrics.histogram("callwrapper", duration, tags)
      }
    }.handleErrorWith { error =>
      Sync[F].delay {
        val duration = ScalaDuration.fromNanos(System.nanoTime() - startTime)
        val tags = Map(
          "name" -> cfg.name,
          "cached" -> cfg.enableCache.toString,
          "circuit_breaker" -> cfg.enableCb.toString,
          "result" -> "failure"
        )
        metrics.histogram("callwrapper", duration, tags)
      } >> Sync[F].raiseError(error)
    }
  }

  private def executeWithCircuitBreaker[A](action: F[A]): F[A] = {
    circuitBreaker match {
      case Some(cb) =>
        Sync[F].delay(cb.executeSupplier(() => action)).flatten.handleErrorWith(handleCircuitBreakerError)
      case None =>
        action
    }
  }

  private def handleCircuitBreakerError[A](error: Throwable): F[A] = error match {
    case _: CallNotPermittedException =>
      logger.warn("Circuit breaker: Call not permitted")
      Sync[F].raiseError(new RuntimeException("Circuit breaker: Call not permitted. Service is unavailable."))
    case other =>
      logger.error(s"Unexpected error: ${other.getMessage}")
      Sync[F].raiseError(new RuntimeException(s"Unexpected error: ${other.getMessage}"))
  }
}