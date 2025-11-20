package app.mosia.nexus
package infrastructure.monitoring

import domain.services.infra.PrometheusExporter
import domain.error.*

import zio.*
import zio.json.*
import zio.http.*
import zio.metrics.connectors.prometheus.PrometheusPublisher

final class PrometheusExporterLive(publisher: PrometheusPublisher) extends PrometheusExporter:

  override def getMetrics: UIO[String] = publisher.get

object PrometheusExporterLive:
  val live: ZLayer[PrometheusPublisher, Nothing, PrometheusExporterLive] =
    ZLayer.fromFunction(new PrometheusExporterLive(_))
