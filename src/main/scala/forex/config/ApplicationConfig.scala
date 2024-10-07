package forex.config

import scala.concurrent.duration.FiniteDuration

case class ApplicationConfig(
    http: HttpConfig,
    oneFrame: OneFrame,
)

case class HttpConfig(
    host: String,
    port: Int,
    timeout: FiniteDuration
)

/**
 * Configuration for the call wrapper.
 *
 * @param name The name of the caller function or use case, will be used on cb
 * @param failureRateThreshold The failure rate threshold (in percentage) to open the circuit breaker
 * @param waitDurationInOpenState The waiting time for the circuit breaker to allow upstream service recovery
 * @param cacheTTL The cache time-to-live
 * @param enableCb Whether to enable the circuit breaker
 * @param enableCache Whether to enable caching
 */
case class CallWrapperConfig(
  name: String,
  failureRateThreshold: Int,
  waitDurationInOpenState: FiniteDuration,
  cacheTTL: FiniteDuration,
  enableCb: Boolean = false,
  enableCache: Boolean = false
)


case class OneFrame (
    http: HttpConfig,
    callWrapper: CallWrapperConfig,
    token: String,
    pairPath: String,
)