package app.mosia.nexus.application.dto.response.auth

import sttp.tapir.Schema
import zio.json.{jsonField, JsonCodec}

case class RefreshTokenResponse(
  @jsonField("access_token") accessToken: String,
  @jsonField("refresh_token") refreshToken: String,
  @jsonField("token_type") tokenType: String = "Bearer",
  @jsonField("expires_in") expiresIn: Long
) derives JsonCodec,
      Schema
