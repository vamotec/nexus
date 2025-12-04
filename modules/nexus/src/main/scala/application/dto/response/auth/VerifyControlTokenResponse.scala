package app.mosia.nexus
package application.dto.response.auth

import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

@jsonMemberNames(SnakeCase)
case class VerifyControlTokenResponse(
  userId: String,
  sessionId: String,
  valid: Boolean
) derives JsonCodec,
      Schema
