package app.mosia.nexus.application.dto.response.auth

import app.mosia.nexus.application.dto.response.user.UserResponse
import sttp.tapir.Schema
import zio.json.{jsonField, JsonCodec}

case class TokenResponse(
  @jsonField("access_token") accessToken: String,
  @jsonField("refresh_token") refreshToken: String,
  @jsonField("token_type") tokenType: String = "Bearer",
  @jsonField("expires_in") expiresIn: Long,
  user: UserResponse
) derives JsonCodec,
      Schema
