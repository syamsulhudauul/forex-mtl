package forex.http
package rates

import forex.domain.Currency.show
import forex.domain.Rate.Pair
import forex.domain._
import io.circe._
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.deriveConfiguredEncoder

object Protocol {

  implicit val configuration: Configuration = Configuration.default.withSnakeCaseMemberNames

  final case class GetApiRequest private (
     from: Currency,
     to: Currency
  )

  object GetApiRequest {
    def apply(from: Currency, to: Currency): Either[String, GetApiRequest] = {
      (from, to) match {
        case (Currency.JPY, Currency.USD) | (Currency.USD, Currency.JPY) =>
          Right(new GetApiRequest(from, to))
        case _ =>
          Left("Unsupported currency pair. Only JPY to USD or USD to JPY is allowed.")
      }
    }

    // If you need to decode this from JSON, you might want to add a custom decoder
    implicit val getApiRequestDecoder: Decoder[GetApiRequest] = new Decoder[GetApiRequest] {
      final def apply(c: HCursor): Decoder.Result[GetApiRequest] =
        for {
          from <- c.downField("from").as[Currency]
          to <- c.downField("to").as[Currency]
          request <- GetApiRequest(from, to).left.map(err => DecodingFailure(err, c.history))
        } yield request
    }
  }

  final case class GetApiResponse(
      from: Currency,
      to: Currency,
      price: Price,
      timestamp: Timestamp
  )

  implicit val currencyEncoder: Encoder[Currency] =
    Encoder.instance[Currency] { show.show _ andThen Json.fromString }

  implicit val pairEncoder: Encoder[Pair] =
    deriveConfiguredEncoder[Pair]

  implicit val rateEncoder: Encoder[Rate] =
    deriveConfiguredEncoder[Rate]

  implicit val responseEncoder: Encoder[GetApiResponse] =
    deriveConfiguredEncoder[GetApiResponse]

}