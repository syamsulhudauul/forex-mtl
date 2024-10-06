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

// * cbName: the name of caller function / usecase
// * failureRateThreshold: the failure rate in percentage to open the CB
// * waitDurationInOpenState: waiting time from CB to let upstream service recover / ready to serve again.
// * cacheTTL: cache time to live in second
case class CallWrapperConfig(
    cbName: String,
    failureRateThreshold: Int,
    waitDurationInOpenState: Long,
    cacheTTL: Long,
    enableCb: Boolean = false, // Optional cb parameter
    enableCache: Boolean = false // Optional cache parameter
)


case class OneFrame (
    http: HttpConfig,
    callWrapper: CallWrapperConfig,
    token: String,
    pairPath: String,
)