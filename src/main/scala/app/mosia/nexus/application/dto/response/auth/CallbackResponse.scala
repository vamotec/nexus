package app.mosia.nexus.application.dto.response.auth

import app.mosia.nexus.application.dto.response.user.UserResponse
import sttp.tapir.Schema
import zio.json.JsonCodec

case class CallbackResponse(
  user: UserResponse,
  platform: Option[String]
) derives JsonCodec,
      Schema
