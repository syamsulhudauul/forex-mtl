package forex

import cats.effect.{ConcurrentEffect, Resource, Timer}
import forex.config.ApplicationConfig
import forex.http.rates.RatesHttpRoutes
import forex.http.metrics.MetricsHttpRoutes
import forex.services._
import forex.programs._
import org.http4s._
import org.http4s.implicits._
import org.http4s.client.Client
import org.http4s.blaze.client.BlazeClientBuilder
import org.http4s.server.middleware.{AutoSlash, Timeout}
import forex.utils.metrics.{Instrumentation, Metrics}
import cats.syntax.semigroupk._
import forex.utils.callWrapper.CallWrapper

import scala.concurrent.ExecutionContext

class Module[F[_]: ConcurrentEffect: Timer](config: ApplicationConfig) {

  private val metrics = Metrics()
  private val instrumentation = Instrumentation[F](metrics)

  // Create an HTTP client
  private val httpClient: Resource[F, Client[F]] = BlazeClientBuilder[F](ExecutionContext.global).resource

  // Use the client to create the ratesService
  private val oneFrameAPICw  = new CallWrapper[F](config.oneFrame.callWrapper,metrics)
  private val ratesService: Resource[F, RatesService[F]] = httpClient.map { client =>
    RatesServices.api[F](client, config, oneFrameAPICw)
  }

  // Use the ratesService to create the ratesProgram
  private val ratesProgram: Resource[F, RatesProgram[F]] = ratesService.map { service =>
    RatesProgram[F](service)
  }

  // Use the ratesProgram to create the HTTP routes
  private val ratesHttpRoutes: Resource[F, HttpRoutes[F]] = ratesProgram.map { program =>
    new RatesHttpRoutes[F](program).routes
  }

  // metrics routes
  private val metricsHttpRoutes: Resource[F, HttpRoutes[F]] = Resource.pure(new MetricsHttpRoutes[F](metrics).routes)

  type PartialMiddleware = HttpRoutes[F] => HttpRoutes[F]
  type TotalMiddleware   = HttpApp[F] => HttpApp[F]

  private val routesMiddleware: PartialMiddleware = {
    { http: HttpRoutes[F] =>
      AutoSlash(http)
    }
  }

  private val appMiddleware: TotalMiddleware = { http: HttpApp[F] =>
    Timeout(config.http.timeout)(http)
    instrumentation.instrumentHttpApp(http)
  }

  // Combine both routes and apply the middleware
  val httpApp: Resource[F, HttpApp[F]] = for {
    ratesRoutes   <- ratesHttpRoutes
    metricsRoutes <- metricsHttpRoutes
    // Combine ratesHttpRoutes and metricsHttpRoutes
    combinedRoutes = ratesRoutes <+> metricsRoutes
  } yield {
    // Apply middlewares to the combined routes
    appMiddleware(routesMiddleware(combinedRoutes).orNotFound)
  }
}
