package app.mosia.nexus.infra.error

import sttp.tapir.Schema
import zio.json.JsonCodec

case class ErrorResponse(
  code: String,
  message: String,
  details: Option[String] = None,
  timestamp: java.time.Instant = java.time.Instant.now()
) derives JsonCodec,
      Schema

object ErrorResponse:
  def from(error: AppError): ErrorResponse =
    ErrorResponse(
      code = error.code,
      message = error.message,
      details = error.details
    )
