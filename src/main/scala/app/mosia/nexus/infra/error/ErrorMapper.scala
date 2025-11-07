package app.mosia.nexus.infra.error

import sttp.model.StatusCode
import zio.json.JsonDecoder

import java.sql.SQLException

object ErrorMapper:
  def toAppError(throwable: Throwable): AppError = throwable match {
    // 已知的AppError直接返回
    case appError: AppError => appError

    // 数据库错误
    case sql: SQLException => ExternalServiceError.DatabaseError(sql)

    // zio-json 解析错误
    case json: JsonDecoder.UnsafeJson => ValidationError.InvalidInput("JSON", json.getMessage)

    // 其他未知错误
    case other => InternalError.UnexpectedError(other)
  }

  // 主要的外部接口 - 直接转换为 ErrorResponse
  def toErrorResponse(throwable: Throwable): ErrorResponse =
    ErrorResponse.from(toAppError(throwable))
