package forex.http.rates

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import forex.domain.Currency
import io.circe.parser.decode

class ProtocolSpec extends AnyFlatSpec with Matchers {

  "GetApiRequest" should "accept JPY to USD currency pair" in {
    val request = Protocol.GetApiRequest(Currency.JPY, Currency.USD)
    request shouldBe a[Right[_, _]]
    request.toOption.get should have(
      Symbol("from") (Currency.JPY),
      Symbol("to") (Currency.USD)
    )
  }

  it should "accept USD to JPY currency pair" in {
    val request = Protocol.GetApiRequest(Currency.USD, Currency.JPY)
    request shouldBe a[Right[_, _]]
    request.toOption.get should have(
      Symbol("from") (Currency.USD),
      Symbol("to") (Currency.JPY)
    )
  }

  it should "reject other currency pairs" in {
    val request = Protocol.GetApiRequest(Currency.USD, Currency.EUR)
    request shouldBe a[Left[_, _]]
    request.swap.getOrElse("Unexpected Right value") should include("Unsupported currency pair")
  }

  "GetApiRequest JSON decoder" should "successfully decode valid currency pairs" in {
    val jsonJpyToUsd = """{"from": "JPY", "to": "USD"}"""
    val jsonUsdToJpy = """{"from": "USD", "to": "JPY"}"""

    val decodedJpyToUsd = decode[Protocol.GetApiRequest](jsonJpyToUsd)
    decodedJpyToUsd shouldBe a[Right[_, _]]
    decodedJpyToUsd.toOption.get should have(
      Symbol("from") (Currency.JPY),
      Symbol("to") (Currency.USD)
    )

    val decodedUsdToJpy = decode[Protocol.GetApiRequest](jsonUsdToJpy)
    decodedUsdToJpy shouldBe a[Right[_, _]]
    decodedUsdToJpy.toOption.get should have(
      Symbol("from") (Currency.USD),
      Symbol("to") (Currency.JPY)
    )
  }

  it should "fail to decode invalid currency pairs" in {
    val jsonInvalidPair = """{"from": "USD", "to": "EUR"}"""

    val decoded = decode[Protocol.GetApiRequest](jsonInvalidPair)
    decoded shouldBe a[Left[_, _]]
    decoded.swap.getOrElse(new Exception("Unexpected Right value")).getMessage should include("Unsupported currency pair")
  }
}