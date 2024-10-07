package forex.http.metrics

import cats.effect.Sync
import org.http4s._
import org.http4s.dsl.Http4sDsl
import forex.utils.metrics.Metrics

class MetricsHttpRoutes[F[_]: Sync](metrics: Metrics) extends Http4sDsl[F] {
  val routes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / "metrics" =>
      Ok(metrics.scrape())
  }
}

object MetricsHttpRoutes {
  def apply[F[_]: Sync](metrics: Metrics): MetricsHttpRoutes[F] =
    new MetricsHttpRoutes[F](metrics)
}