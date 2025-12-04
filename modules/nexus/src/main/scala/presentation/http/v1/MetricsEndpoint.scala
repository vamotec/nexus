package app.mosia.nexus
package presentation.http.v1

import domain.config.AppConfig
import domain.services.infra.{JwtContent, PrometheusExporter}
import app.mosia.nexus.presentation.http.EndpointModule
import app.mosia.nexus.presentation.http.SecureEndpoints.baseEndpoint

import sttp.capabilities.zio.ZioStreams
import sttp.model.StatusCode
import sttp.tapir.json.zio.jsonBody
import sttp.tapir.ztapir.*
import sttp.tapir.{Endpoint, EndpointOutput}
import zio.ZIO

final class MetricsEndpoint(config: AppConfig, prometheus: PrometheusExporter) extends EndpointModule:
  override def publicEndpoints: List[ZServerEndpoint[Any, ZioStreams]] =
    if config.monitoring.prometheus.enabled then List(systemMetrics)
    else List.empty

  override def secureEndpoints: List[ZServerEndpoint[JwtContent, ZioStreams]] = List.empty

  private val systemMetrics: ZServerEndpoint[Any, ZioStreams] =
    baseEndpoint.get
      .in("metrics")
      .out(stringBody)
      .out(header[String]("Content-Type"))
      .zServerLogic { _ =>
        for metrics <- prometheus.getMetrics
        yield (metrics, "text/plain; charset=utf-8")
      }
