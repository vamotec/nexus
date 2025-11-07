package app.mosia.nexus.presentation.http.endpoint

import app.mosia.nexus.infra.error.{AppError, ErrorMapper, ErrorResponse}
import sttp.model.StatusCode
import sttp.tapir.json.zio.jsonBody
import sttp.tapir.{oneOf, oneOfVariant, EndpointOutput}
import zio.ZIO

object TapirErrorHandling:
  val commonErrorOutput: EndpointOutput.OneOf[ErrorResponse, ErrorResponse] = oneOf[ErrorResponse](
    oneOfVariant(StatusCode.BadRequest, jsonBody[ErrorResponse].description("Validation error")),
    oneOfVariant(StatusCode.Unauthorized, jsonBody[ErrorResponse].description("Authentication error")),
    oneOfVariant(StatusCode.Forbidden, jsonBody[ErrorResponse].description("Authorization error")),
    oneOfVariant(StatusCode.NotFound, jsonBody[ErrorResponse].description("Resource not found")),
    oneOfVariant(StatusCode.Conflict, jsonBody[ErrorResponse].description("Resource conflict")),
    oneOfVariant(StatusCode.InternalServerError, jsonBody[ErrorResponse].description("Internal server error")),
    oneOfVariant(StatusCode.BadGateway, jsonBody[ErrorResponse].description("External service error"))
  )

  // 安全的服务器逻辑
  def secureLogic[R, I, O](
    logic: I => ZIO[R, AppError, O]
  ): I => ZIO[R, ErrorResponse, O] = // 移除了 StatusCode，只需要 ErrorResponse
    input =>
      logic(input).mapError { error =>
        ErrorMapper.toErrorResponse(error) // 直接返回 ErrorResponse
      }
