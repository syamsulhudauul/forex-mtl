package forex.utils.metrics

import io.micrometer.core.instrument.{Metrics => Micrometer, Tag}
import io.micrometer.prometheus.{PrometheusConfig, PrometheusMeterRegistry}
import scala.jdk.CollectionConverters._
import scala.concurrent.duration.Duration

class Metrics {
  private val registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
  Micrometer.globalRegistry.add(registry)

  def histogram(metricName: String, duration: Duration, tags: Map[String, String]): Unit = {
    val tagList = tags.map { case (k, v) => Tag.of(k, v) }.toList.asJava
    registry.timer(metricName, tagList).record(java.time.Duration.ofNanos(duration.toNanos))
  }

  def scrape(): String = registry.scrape()
}

object Metrics {
  def apply(): Metrics = new Metrics()
}