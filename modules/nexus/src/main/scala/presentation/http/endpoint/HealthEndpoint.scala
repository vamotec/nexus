package app.mosia.nexus
package presentation.http.endpoint

import domain.model.health.{HealthCheckResult, HealthStatus}
import domain.services.infra.{HealthCheckService, JwtService, PrometheusExporter}
import application.dto.response.common.ApiResponse
import domain.config.AppConfig
import domain.error.*
import presentation.http.endpoint.SecureEndpoints.baseEndpoint

import sttp.capabilities.zio.ZioStreams
import sttp.model.StatusCode
import sttp.tapir.{Endpoint, EndpointOutput}
import sttp.tapir.json.zio.jsonBody
import sttp.tapir.ztapir.*
import zio.ZIO

final class HealthEndpoint(config: AppConfig, healthService: HealthCheckService) extends EndpointModule:
  override def publicEndpoints: List[ZServerEndpoint[Any, ZioStreams]] =
    if config.monitoring.healthCheck.enabled then List(health)
    else List.empty

  override def secureEndpoints: List[ZServerEndpoint[JwtService, ZioStreams]] = List.empty

  private val health: ZServerEndpoint[Any, ZioStreams] =
    baseEndpoint.get
      .in("health")
      .out(statusCode.and(jsonBody[ApiResponse[HealthCheckResult]]))
      .zServerLogic { _ =>
        (for
          result <- healthService.checkHealth()
          code = result.status match
            case HealthStatus.Healthy => StatusCode.Ok
            case HealthStatus.Degraded => StatusCode.Ok // 或使用 207 Multi-Status
            case HealthStatus.Unhealthy => StatusCode.ServiceUnavailable
        yield (code, ApiResponse(result))).mapError(toErrorResponse)
      }
