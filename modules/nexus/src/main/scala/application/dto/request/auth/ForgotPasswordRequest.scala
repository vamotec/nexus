package app.mosia.nexus
package application.dto.request.auth

import sttp.tapir.Schema
import zio.json.*

case class ForgotPasswordRequest(
  email: String
) derives JsonCodec,
      Schema
