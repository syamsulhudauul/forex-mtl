package forex.services.rates

import forex.config.ApplicationConfig
import cats.Applicative
import cats.effect.Sync
import forex.utils.callWrapper.CallWrapper
import interpreters._
import org.http4s.client.Client

object Interpreters {
  def dummy[F[_]: Applicative]: Algebra[F] = new OneFrameDummy[F]()
  def api[F[_]: Sync](client: Client[F], config: ApplicationConfig, callWrapper: CallWrapper[F]): Algebra[F] = new OneFrameAPI[F](client, config, callWrapper)
}
