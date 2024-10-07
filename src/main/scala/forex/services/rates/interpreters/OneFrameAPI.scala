package forex.services.rates.interpreters

import forex.domain.{Currency, Price, Rate, Timestamp}
import forex.services.rates.errors._
import forex.services.rates.Algebra
import forex.config.ApplicationConfig
import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.applicativeError._
import cats.syntax.either._
import cats.syntax.applicative._
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.Method._
import org.http4s._
import io.circe.generic.auto._
import org.typelevel.ci.CIString
import org.slf4j.{Logger, LoggerFactory}
import forex.utils.callWrapper.CallWrapper
import forex.utils.metrics.Metrics

// Define a case class to map the JSON response from the API
final case class RateResponse(
 from: Currency,
 to: Currency,
 price: BigDecimal,
 time_stamp: String
)

class OneFrameAPI[F[_]: Sync](client: Client[F], config: ApplicationConfig) extends Algebra[F] with Http4sClientDsl[F] {
  // Build the request URI based on the pair
  private def buildUri(baseUri:Uri,pair: Rate.Pair): Uri = baseUri.withQueryParam("pair", s"${pair.from}${pair.to}")
  private val token: String = config.oneFrame.token
  private val baseUri: Uri = Uri.unsafeFromString(s"${config.oneFrame.http.host}:${config.oneFrame.http.port}${config.oneFrame.pairPath}")
  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  private val metrics = new Metrics()
  private val callWrapper = new CallWrapper[F](config.oneFrame.callWrapper,metrics)

  override def get(pair: Rate.Pair): F[Error Either Rate] = {

    // Create an HTTP request with headers
    val request = GET(
      buildUri(baseUri, pair),
      Header.Raw.apply(CIString("token"), token)
    )

    // Log the request details (URI and headers)
    logger.debug(s"Created request with URI: ${request.uri}")
    logger.debug(s"Headers: ${request.headers}")
    val key = s"${pair.from}${pair.to}"

    // callWrapper to apply memcache n cb
    callWrapper.call(key) {
      // Send the request and handle the response
      client.run(request).use { response =>
        response.status match {
          case Status.Ok =>
            response.as[List[RateResponse]].attempt.flatMap {
              case Right(rateResponses) =>
                logger.debug(s"Successfully decoded response: ${rateResponses}")
                val rateResponse = rateResponses.head
                val rate = Rate(
                  pair = Rate.Pair(rateResponse.from, rateResponse.to),
                  price = Price(rateResponse.price),
                  timestamp = Timestamp.now // You can parse `rateResponse.time_stamp` here
                )
                rate.asRight[Error].pure[F]

              case Left(error) =>
                logger.error(s"Failed to decode response: ${error.getMessage}")
                Error.OneFrameLookupFailed("Failed to decode API response").asLeft[Rate].leftMap(identity[Error]).pure[F]
            }

          case Status.BadRequest =>
            logger.debug("400")
            Error.OneFrameLookupFailed("Bad Request").asLeft[Rate].leftMap(identity[Error]).pure[F]

          case Status.Unauthorized =>
            logger.debug("403")
            Error.OneFrameLookupFailed("Unauthorized").asLeft[Rate].leftMap(identity[Error]).pure[F]
          case unexpectedStatus =>
            logger.debug(s"Unexpected status code: $unexpectedStatus")
            Error.OneFrameLookupFailed("Unexpected response from the API").asLeft[Rate].leftMap(identity[Error]).pure[F]
        }
      }
    }
  }
}