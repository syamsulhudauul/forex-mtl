//package forex.utils.callWrapper
//
//import cats.effect.IO
//import cats.effect.Sync
//import cats.effect.testing.scalatest.AsyncIOSpec
//import forex.config.CallWrapperConfig
//import forex.utils.metrics.Metrics
//import org.scalatest.freespec.AsyncFreeSpec
//import org.scalatest.matchers.should.Matchers
//import org.scalatestplus.mockito.MockitoSugar
//import org.mockito.Mockito._
//import org.mockito.ArgumentMatchers._
//
//import scala.concurrent.duration._
//
//class CallWrapperSpec extends AsyncFreeSpec with AsyncIOSpec with Matchers with MockitoSugar {
//
//  val mockMetrics: Metrics = mock[Metrics]
//
//  val config = CallWrapperConfig(
//    name = "test-wrapper",
//    failureRateThreshold = 50,
//    waitDurationInOpenState = 1.minute,
//    cacheTTL = 5.minutes,
//    enableCb = true,
//    enableCache = true
//  )
//
//  def createCallWrapper(cfg: CallWrapperConfig = config): CallWrapper[IO] = {
//    implicit val syncIO: Sync[IO] = IO.asyncForIO
//    new CallWrapper[IO](cfg, mockMetrics)
//  }
//
//  "CallWrapper" - {
//    "should execute the action and return the result" in {
//      val callWrapper = createCallWrapper()
//      callWrapper.call("test-key")(IO.pure("test-result")).asserting(_ shouldBe "test-result")
//    }
//
//    "should cache results when caching is enabled" in {
//      val callWrapper = createCallWrapper()
//      var callCount = 0
//      val action = IO {
//        callCount += 1
//        "test-result"
//      }
//
//      for {
//        _ <- callWrapper.call("test-key")(action)
//        _ <- callWrapper.call("test-key")(action)
//        _ <- IO(callCount shouldBe 1)
//      } yield succeed
//    }
//
//    "should not cache results when caching is disabled" in {
//      val configWithoutCache = config.copy(enableCache = false)
//      val callWrapper = createCallWrapper(configWithoutCache)
//      var callCount = 0
//      val action = IO {
//        callCount += 1
//        "test-result"
//      }
//
//      for {
//        _ <- callWrapper.call("test-key")(action)
//        _ <- callWrapper.call("test-key")(action)
//        _ <- IO(callCount shouldBe 2)
//      } yield succeed
//    }
//
//    "should record metrics" in {
//      val callWrapper = createCallWrapper()
//      callWrapper.call("test-key")(IO.pure("test-result")).map { _ =>
//        verify(mockMetrics, times(1)).histogram(
//          org.mockito.ArgumentMatchers.eq("callwrapper"),
//          any[Duration],
//          any[Map[String, String]]
//        )
//        succeed
//      }
//    }
//
//    "should handle errors and record error metrics" in {
//      val callWrapper = createCallWrapper()
//      val errorAction = IO.raiseError[String](new RuntimeException("Test error"))
//
//      callWrapper.call("test-key")(errorAction).attempt.map { result =>
//        result.isLeft shouldBe true
//        verify(mockMetrics, times(1)).histogram(
//          org.mockito.ArgumentMatchers.eq("callwrapper"),
//          any[Duration],
//          org.mockito.ArgumentMatchers.argThat[(Map[String, String])] { tags =>
//            tags("error") == "true"
//          }
//        )
//        succeed
//      }
//    }
//  }
//}