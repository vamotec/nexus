package app.mosia.nexus
package application.dto.request.auth

import domain.model.verification.VerificationCodeType
import sttp.tapir.Schema
import zio.json.*

case class ResendVerificationCodeRequest(
  email: String,
  codeType: VerificationCodeType
) derives JsonCodec,
      Schema
