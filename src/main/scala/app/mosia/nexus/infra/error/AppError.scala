package app.mosia.nexus.infra.error

import sttp.model.StatusCode

sealed trait AppError extends Throwable:
  def message: String
  def code: String
  def httpStatus: StatusCode
  def details: Option[String] = None

object AppError:
  // 错误分类
  trait ClientError extends AppError // 4xx 错误
  trait ServerError extends AppError // 5xx 错误
  trait ExternalServiceError extends ServerError
