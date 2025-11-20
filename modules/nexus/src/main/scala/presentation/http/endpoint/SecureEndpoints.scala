package app.mosia.nexus
package presentation.http.endpoint

import domain.error.ErrorResponse
import domain.services.infra.JwtService
import domain.model.user.UserId

import sttp.model.StatusCode
import sttp.tapir.{Endpoint, EndpointOutput}
import sttp.tapir.json.zio.jsonBody
import sttp.tapir.ztapir.*
import zio.ZIO

object SecureEndpoints:
  private val commonErrorOutput: EndpointOutput.OneOf[ErrorResponse, ErrorResponse] = oneOf[ErrorResponse](
    oneOfVariant(StatusCode.BadRequest, jsonBody[ErrorResponse].description("Validation error")),
    oneOfVariant(StatusCode.Unauthorized, jsonBody[ErrorResponse].description("Authentication error")),
    oneOfVariant(StatusCode.Forbidden, jsonBody[ErrorResponse].description("Authorization error")),
    oneOfVariant(StatusCode.NotFound, jsonBody[ErrorResponse].description("Resource not found")),
    oneOfVariant(StatusCode.Conflict, jsonBody[ErrorResponse].description("Resource conflict")),
    oneOfVariant(StatusCode.InternalServerError, jsonBody[ErrorResponse].description("Internal server error")),
    oneOfVariant(StatusCode.BadGateway, jsonBody[ErrorResponse].description("External service error"))
  )
  // 基础端点定义
  val baseEndpoint: Endpoint[Unit, Unit, ErrorResponse, Unit, Any] =
    endpoint.errorOut(commonErrorOutput)

  val secureBase: ZPartialServerEndpoint[JwtService, String, UserId, Unit, ErrorResponse, Unit, Any] =
    baseEndpoint
      .securityIn(auth.bearer[String]())
      .zServerSecurityLogic { token =>
        ZIO
          .serviceWithZIO[JwtService](_.validateToken(token))
          .mapError(_.toErrorResponse)
      }
