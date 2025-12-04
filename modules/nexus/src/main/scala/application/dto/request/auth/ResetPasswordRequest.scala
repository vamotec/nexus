package app.mosia.nexus
package application.dto.request.auth

import sttp.tapir.Schema
import zio.json.*

case class ResetPasswordRequest(
  email: String,
  code: String,
  token: String,
  newPassword: String
) derives JsonCodec,
      Schema
