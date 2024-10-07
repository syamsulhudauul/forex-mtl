package forex.utils.metrics

import cats.effect._
import cats.implicits._
import org.http4s.{HttpApp, Request, Response}
import scala.concurrent.duration.Duration

class Instrumentation[F[_]: Sync] (metrics: Metrics) {

  def instrumentHttpApp(httpApp: HttpApp[F]): HttpApp[F] = HttpApp[F] { req =>
    val routeName = getRouteName(req)
    val startTime = System.nanoTime()

    httpApp.run(req).attempt.flatMap {
      case Right(resp) =>
        recordMetrics(req, resp, routeName, startTime).as(resp)
      case Left(error) =>
        recordErrorMetrics(req, routeName, startTime) *> Sync[F].raiseError[Response[F]](error)
    }
  }

  private def getRouteName(req: Request[F]): String = {
    req.pathInfo.segments.map(_.toString).filter(_.nonEmpty) match {
      case segments if segments.isEmpty => "root"
      case segments => segments.mkString("_")
    }
  }

  private def recordMetrics(req: Request[F], resp: Response[F], routeName: String, startTime: Long): F[Unit] =
    Sync[F].delay {
      val duration = Duration.fromNanos(System.nanoTime() - startTime)
      val success = resp.status.isSuccess
      val tags = Map(
        "route" -> routeName,
        "method" -> req.method.name,
        "status" -> resp.status.code.toString,
        "result" -> (if (success) "success" else "failure")
      )
      metrics.histogram("http_request_duration", duration, tags)
    }

  private def recordErrorMetrics(req: Request[F], routeName: String, startTime: Long): F[Unit] =
    Sync[F].delay {
      val duration = Duration.fromNanos(System.nanoTime() - startTime)
      val tags = Map(
        "route" -> routeName,
        "method" -> req.method.name,
        "status" -> "500", // Assuming server error for exceptions
        "result" -> "failure"
      )
      metrics.histogram("http_request_duration", duration, tags)
    }
}

object Instrumentation {
  def apply[F[_]: Sync](metrics: Metrics): Instrumentation[F] = new Instrumentation[F](metrics)
}