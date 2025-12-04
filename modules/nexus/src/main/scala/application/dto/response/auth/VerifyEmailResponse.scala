package app.mosia.nexus
package application.dto.response.auth

import sttp.tapir.Schema
import zio.json.*

@jsonMemberNames(SnakeCase)
case class VerifyEmailResponse(
  success: Boolean,
  message: String
) derives JsonCodec,
      Schema
