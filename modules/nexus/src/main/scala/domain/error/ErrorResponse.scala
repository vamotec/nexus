package app.mosia.nexus
package domain.error

import sttp.tapir.Schema
import zio.json.*

case class ErrorResponse(
  code: String,
  message: String,
  timestamp: java.time.Instant = java.time.Instant.now()
) derives JsonCodec,
      Schema

object ErrorResponse:
  def from(error: AppError): ErrorResponse =
    ErrorResponse(
      code = error.errorCode,
      message = error.message
    )
