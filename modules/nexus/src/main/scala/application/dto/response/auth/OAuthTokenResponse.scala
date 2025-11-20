package app.mosia.nexus
package application.dto.response.auth

import application.dto.response.user.UserResponse

import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

case class OAuthTokenResponse(
  @jsonField("access_token") accessToken: String,
  @jsonField("token_type") tokenType: String,
  @jsonField("expires_in") expiresIn: Option[Int] = None,
  @jsonField("refresh_token") refreshToken: Option[String] = None,
  scope: Option[String] = None
) derives JsonCodec,
      Schema
