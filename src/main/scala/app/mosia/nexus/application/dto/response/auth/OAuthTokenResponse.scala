package app.mosia.nexus.application.dto.response.auth

import app.mosia.nexus.application.dto.response.user.UserResponse
import sttp.tapir.Schema
import zio.json.{jsonField, JsonCodec}

case class OAuthTokenResponse(
  @jsonField("access_token") accessToken: String,
  @jsonField("token_type") tokenType: String,
  @jsonField("expires_in") expiresIn: Option[Int] = None,
  @jsonField("refresh_token") refreshToken: Option[String] = None,
  scope: Option[String] = None
) derives JsonCodec,
      Schema
