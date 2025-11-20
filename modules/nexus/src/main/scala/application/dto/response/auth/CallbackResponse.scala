package app.mosia.nexus
package application.dto.response.auth

import application.dto.response.user.UserResponse

import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

case class CallbackResponse(
  user: UserResponse,
  platform: Option[String]
) derives JsonCodec,
      Schema
