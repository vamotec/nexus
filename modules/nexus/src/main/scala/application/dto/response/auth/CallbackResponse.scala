package app.mosia.nexus
package application.dto.response.auth

import application.dto.response.user.UserResponse

import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

@jsonMemberNames(SnakeCase)
case class CallbackResponse(
  user: UserResponse,
  redirectUri: String,
  platform: Option[String]
) derives JsonCodec,
      Schema
