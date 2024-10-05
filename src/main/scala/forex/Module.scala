package forex

import cats.effect.{ Timer, ConcurrentEffect, Resource }
import forex.config.ApplicationConfig
import forex.http.rates.RatesHttpRoutes
import forex.services._
import forex.programs._
import org.http4s._
import org.http4s.implicits._
import org.http4s.client.Client
import org.http4s.blaze.client.BlazeClientBuilder
import org.http4s.server.middleware.{ AutoSlash, Timeout }

import scala.concurrent.ExecutionContext

class Module[F[_]: ConcurrentEffect: Timer](config: ApplicationConfig) {

  // Create an HTTP client
  private val httpClient: Resource[F, Client[F]] = BlazeClientBuilder[F](ExecutionContext.global).resource

  // Use the client to create the ratesService
  private val ratesService: Resource[F, RatesService[F]] = httpClient.map { client =>
    RatesServices.api[F](client, config)
  }

  // Use the ratesService to create the ratesProgram
  private val ratesProgram: Resource[F, RatesProgram[F]] = ratesService.map { service =>
    RatesProgram[F](service)
  }

  // Use the ratesProgram to create the HTTP routes
  private val ratesHttpRoutes: Resource[F, HttpRoutes[F]] = ratesProgram.map { program =>
    new RatesHttpRoutes[F](program).routes
  }


  type PartialMiddleware = HttpRoutes[F] => HttpRoutes[F]
  type TotalMiddleware   = HttpApp[F] => HttpApp[F]

  private val routesMiddleware: PartialMiddleware = {
    { http: HttpRoutes[F] =>
      AutoSlash(http)
    }
  }

  private val appMiddleware: TotalMiddleware = { http: HttpApp[F] =>
    Timeout(config.http.timeout)(http)
  }

  val httpApp: Resource[F, HttpApp[F]] = ratesHttpRoutes.map { http =>
    appMiddleware(routesMiddleware(http).orNotFound)
  }
}
