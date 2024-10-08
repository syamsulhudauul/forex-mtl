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

class CallWrapper[F[_]](cfg: CallWrapperConfig, metrics: Metrics)(implicit F: Sync[F]){
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

    def handleMetrics(cached: Boolean, errorOccurred: Boolean): F[Unit] = {
      val duration = ScalaDuration.fromNanos(System.nanoTime() - startTime)
      val tags = Map(
        "name" -> cfg.name,
        "cached" -> cached.toString,
        "circuit_breaker" -> circuitBreaker.flatMap(cb => Option(cb.getState)).getOrElse("unknown").toString,
        "error" -> errorOccurred.toString
      )
      logger.info(s"Logging metrics for callWrapper: duration=$duration, tags=$tags")
      Sync[F].delay(metrics.histogram("callwrapper", duration, tags))
    }

    def handleError(error: Throwable, cached: Boolean): F[A] = {
      handleMetrics(cached, errorOccurred = true) *> Sync[F].raiseError[A](error)
    }

    if (cfg.enableCache) {
      for {
        currentCache <- cache.get
        result <- currentCache.get(key) match {
          case Some((timestamp, value)) if Instant.now().isBefore(timestamp.plusSeconds(cfg.cacheTTL.toSeconds)) =>
            handleMetrics(cached = true, errorOccurred = false) *> Sync[F].pure(value.asInstanceOf[A])
          case _ =>
            executeWithCircuitBreaker(action).handleErrorWith(err => handleError(err, cached = false)).flatMap { result =>
              cache.update(_ + (key -> (Instant.now(), result))) *> handleMetrics(cached = false, errorOccurred = false).as(result)
            }
        }
      } yield result
    } else {
      executeWithCircuitBreaker(action)
        .handleErrorWith(err => handleError(err, cached = false))
        .flatMap(result => handleMetrics(cached = false, errorOccurred = false).as(result))
    }
  }

  private def executeWithCircuitBreaker[A](action: F[A]): F[A] = {
    circuitBreaker match {
      case Some(cb) =>
        Sync[F].delay(cb.executeSupplier(() => action)).flatten.handleErrorWith(handleCircuitBreakerError[A])
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
