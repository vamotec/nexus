package app.mosia.nexus.presentation.http.endpoint

import app.mosia.nexus.domain.model.user.UserId
import app.mosia.nexus.infra.auth.JwtService
import app.mosia.nexus.infra.error.{ErrorMapper, ErrorResponse}
import sttp.tapir.*
import sttp.tapir.ztapir.{RichZEndpoint, ZPartialServerEndpoint}
import zio.ZIO

object SecureEndpoints:
  // 基础端点定义
  val baseEndpoint: Endpoint[Unit, Unit, ErrorResponse, Unit, Any] =
    endpoint.errorOut(TapirErrorHandling.commonErrorOutput)

  val secureBase: ZPartialServerEndpoint[JwtService, String, UserId, Unit, ErrorResponse, Unit, Any] =
    baseEndpoint
      .securityIn(auth.bearer[String]())
      .zServerSecurityLogic { token =>
        ZIO
          .serviceWithZIO[JwtService](_.validateToken(token))
          .mapError(ErrorMapper.toErrorResponse)
      }
