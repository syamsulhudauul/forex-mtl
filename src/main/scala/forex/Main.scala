package forex

import scala.concurrent.ExecutionContext
import cats.effect._
import forex.config._
import fs2.Stream
import org.http4s.blaze.server.BlazeServerBuilder
import org.slf4j.LoggerFactory
import org.http4s.server.middleware.Logger

object Main extends IOApp {
  private val logger = LoggerFactory.getLogger(getClass)

  override def run(args: List[String]): IO[ExitCode] = {
    logger.info("Starting application")
    new Application[IO].stream(executionContext).compile.drain.as(ExitCode.Success)
  }
}

class Application[F[_]: ConcurrentEffect: Timer] {
  def stream(ec: ExecutionContext): Stream[F, Unit] =
    for {
      config <- Config.stream("app")
      module = new Module[F](config)

      httpApp <- Stream.resource(module.httpApp)
      loggingMiddleware = Logger.httpApp[F](logHeaders = true, logBody = true)(httpApp)
      _ <- BlazeServerBuilder[F](ec)
        .bindHttp(config.http.port, config.http.host)
        .withHttpApp(loggingMiddleware)
        .serve
    } yield ()
}