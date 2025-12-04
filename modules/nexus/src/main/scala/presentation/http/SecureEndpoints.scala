package app.mosia.nexus
package presentation.http

import domain.error.ClientError.NotFound
import domain.error.ErrorResponse
import domain.model.user.UserId
import domain.services.infra.JwtContent

import sttp.model.StatusCode
import sttp.tapir.json.zio.jsonBody
import sttp.tapir.ztapir.*
import sttp.tapir.{Endpoint, EndpointInput, EndpointOutput}
import zio.ZIO
import zio.http.Response

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
    endpoint.errorOut(commonErrorOutput).in("v1")

  val secureBase: ZPartialServerEndpoint[JwtContent, Unit, String, Unit, ErrorResponse, Unit, Any] =
    baseEndpoint
      .zServerSecurityLogic { _ =>
        (for
          userIdOpt <- ZIO.serviceWithZIO[JwtContent](_.get)
          userId <- ZIO.fromOption(userIdOpt).mapError(_ => NotFound("content", "userId"))
        yield userId).mapError(_.toErrorResponse)
      }
    
  val platformEndpoint: Endpoint[Unit, Unit, ErrorResponse, Unit, Any] =
    endpoint.errorOut(commonErrorOutput).in("platform")

  val platformSecure: ZPartialServerEndpoint[JwtContent, Unit, String, Unit, ErrorResponse, Unit, Any] =
    baseEndpoint
      .zServerSecurityLogic { _ =>
        (for
          userIdOpt <- ZIO.serviceWithZIO[JwtContent](_.get)
          userId <- ZIO.fromOption(userIdOpt).mapError(_ => NotFound("content", "userId"))
        yield userId).mapError(_.toErrorResponse)
      }