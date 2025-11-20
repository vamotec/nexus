package app.mosia.nexus
package application.dto.response.auth

import application.dto.response.user.UserResponse
import domain.model.user.TokenPair

import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

case class LoginResponse(
  @jsonField("access_token") accessToken: String,
  @jsonField("refresh_token") refreshToken: String,
  @jsonField("token_type") tokenType: String = "Bearer",
  @jsonField("expires_in") expiresIn: Long,
  user: UserResponse
) derives JsonCodec,
      Schema
