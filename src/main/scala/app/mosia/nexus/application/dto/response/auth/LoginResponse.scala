package app.mosia.nexus.application.dto.response.auth

import app.mosia.nexus.application.dto.response.user.UserResponse
import app.mosia.nexus.domain.model.user.TokenPair
import sttp.tapir.Schema
import zio.json.{jsonField, JsonCodec}

case class LoginResponse(
  @jsonField("access_token") accessToken: String,
  @jsonField("refresh_token") refreshToken: String,
  @jsonField("token_type") tokenType: String = "Bearer",
  @jsonField("expires_in") expiresIn: Long,
  user: UserResponse
) derives JsonCodec,
      Schema
