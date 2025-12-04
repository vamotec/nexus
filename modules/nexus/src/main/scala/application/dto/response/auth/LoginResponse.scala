package app.mosia.nexus
package application.dto.response.auth

import application.dto.response.user.UserResponse
import domain.model.user.TokenPair

import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

@jsonMemberNames(SnakeCase)
case class LoginResponse(
  accessToken: String, 
  refreshToken: String,
  tokenType: String = "Bearer",
  expiresIn: Long,
  user: UserResponse
) derives JsonCodec,
      Schema
