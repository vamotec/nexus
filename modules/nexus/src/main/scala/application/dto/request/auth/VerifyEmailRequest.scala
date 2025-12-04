package app.mosia.nexus
package application.dto.request.auth

import sttp.tapir.Schema
import zio.json.*

case class VerifyEmailRequest(
  email: String,
  code: String,
  token: String
) derives JsonCodec,
      Schema
