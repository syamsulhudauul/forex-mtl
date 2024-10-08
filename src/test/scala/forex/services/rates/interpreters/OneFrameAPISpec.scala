// package forex.services.rates.interpreters

//import cats.effect.IO
//import cats.effect.unsafe.implicits.global
//import forex.config.{ApplicationConfig, HttpConfig, OneFrame, CallWrapperConfig}
//import forex.domain.{Currency, Rate}
//import forex.services.rates.errors.Error
//import forex.utils.callWrapper.CallWrapper
//import forex.utils.metrics.Metrics
//import org.http4s.{Header, Headers, HttpRoutes, Response, Status}
//import org.http4s.client.Client
//import org.http4s.implicits._
//import org.scalatest.flatspec.AnyFlatSpec
//import org.scalatest.matchers.should.Matchers
//import org.scalatestplus.mockito.MockitoSugar
//import org.mockito.Mockito._
//import org.mockito.ArgumentMatchers._
//import io.circe.syntax._
//import io.circe.generic.auto._
//import io.circe.Encoder
//import io.circe.generic.semiauto.deriveEncoder
//import org.typelevel.ci.CIString
//import org.http4s.circe._
//
//import scala.concurrent.duration._
//
//class OneFrameAPISpec extends AnyFlatSpec with Matchers with MockitoSugar {
//
//  // Mock dependencies
//  val mockConfig: ApplicationConfig = mock[ApplicationConfig]
//  val mockMetrics: Metrics = mock[Metrics]
//
//  // Set up config
//  val callWrapperConfig = CallWrapperConfig(
//    name = "one-frame",
//    failureRateThreshold = 50,
//    waitDurationInOpenState = 1.minute,
//    cacheTTL = 5.minutes,
//    enableCb = false,
//    enableCache = false
//  )
//
//  val oneFrameConfig = OneFrame(
//    http = HttpConfig("localhost", 8080, 5.seconds),
//    callWrapper = callWrapperConfig,
//    token = "token123",
//    pairPath = "/rates"
//  )
//  when(mockConfig.oneFrame).thenReturn(oneFrameConfig)
//
//  // Create a mock CallWrapper
//  val mockCallWrapper: CallWrapper[IO] = mock[CallWrapper[IO]]
//
//  implicit val rateResponseEncoder: Encoder[RateResponse] = deriveEncoder[RateResponse]
//
//  // Helper function to create a test HTTP client
//  def createTestClient(response: IO[Response[IO]]): Client[IO] = {
//    Client.fromHttpApp(HttpRoutes.of[IO] { case _ => response }.orNotFound)
//  }
//
//  // Test successful rate retrieval
//  "OneFrameAPI" should "successfully retrieve a rate" in {
//    val pair = Rate.Pair(Currency.USD, Currency.EUR)
//    val rateResponse = RateResponse(Currency.USD, Currency.EUR, BigDecimal(0.82), "2023-05-01T12:00:00Z")
//    val response = IO.pure(Response[IO](Status.Ok).withEntity(List(rateResponse).asJson))
//    val client = createTestClient(response)
//
////    val expectedRate = Rate(pair, Price(BigDecimal(0.82)), Timestamp.now)
//    when(mockCallWrapper.call(any[String])(any[IO[Either[Error, Rate]]])).thenAnswer { invocation =>
//      invocation.getArgument[IO[Either[Error, Rate]]](1)
//    }
//
//    val oneFrameAPI = new OneFrameAPI[IO](client, mockConfig, mockCallWrapper)
//    val result = oneFrameAPI.get(pair).unsafeRunSync()
//
//    result shouldBe a[Right[_, _]]
//    result.toOption.get.pair shouldBe pair
//    result.toOption.get.price.value shouldBe BigDecimal(0.82)
//  }
//
//  // Test correct header setting
//  it should "set the correct headers" in {
//    val pair = Rate.Pair(Currency.USD, Currency.EUR)
//    var capturedHeaders: Headers = Headers.empty
//
//    val client = Client.fromHttpApp(HttpRoutes.of[IO] { case req =>
//      capturedHeaders = req.headers
//      IO.pure(Response[IO](Status.Ok).withEntity(List(RateResponse(Currency.USD, Currency.EUR, BigDecimal(0.82), "2023-05-01T12:00:00Z")).asJson))
//    }.orNotFound)
//
//    when(mockCallWrapper.call(any[String])(any[IO[Either[Error, Rate]]])).thenAnswer { invocation =>
//      invocation.getArgument[IO[Either[Error, Rate]]](1)
//    }
//
//    val oneFrameAPI = new OneFrameAPI[IO](client, mockConfig, mockCallWrapper)
//    oneFrameAPI.get(pair).unsafeRunSync()
//
//    capturedHeaders.get(CIString("token")) should be(Some(Header.Raw(CIString("token"), "token123")))
//  }
//}