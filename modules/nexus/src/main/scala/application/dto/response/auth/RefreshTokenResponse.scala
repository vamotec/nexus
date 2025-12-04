package app.mosia.nexus
package application.dto.response.auth

import sttp.tapir.Schema
import zio.json.*
import zio.*

@jsonMemberNames(SnakeCase)
case class RefreshTokenResponse(
  accessToken: String,
  refreshToken: String,
  tokenType: String = "Bearer",
  expiresIn: Long
) derives JsonCodec,
      Schema
