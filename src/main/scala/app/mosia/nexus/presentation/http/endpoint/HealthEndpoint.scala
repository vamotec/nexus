package app.mosia.nexus.presentation.http.endpoint

import app.mosia.nexus.application.dto.response.common.ApiResponse
import app.mosia.nexus.infra.auth.JwtService
import app.mosia.nexus.presentation.http.endpoint.SecureEndpoints.baseEndpoint
import sttp.capabilities.zio.ZioStreams
import sttp.tapir.json.zio.jsonBody
import sttp.tapir.ztapir.{stringToPath, RichZEndpoint, ZServerEndpoint}
import zio.ZIO

final class HealthEndpoint extends EndpointModule:
  override def endpoints: List[ZServerEndpoint[JwtService, ZioStreams]] =
    List(health)

  val health: ZServerEndpoint[JwtService, ZioStreams] =
    baseEndpoint.get
      .in("health")
      .out(jsonBody[ApiResponse[String]])
      .serverLogicSuccess { _ =>
        ZIO.succeed(ApiResponse("OK"))
      }
