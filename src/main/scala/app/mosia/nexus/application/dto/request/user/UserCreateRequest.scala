package app.mosia.nexus.application.dto.request.user

import sttp.tapir.Schema
import zio.json.JsonCodec

case class UserCreateRequest(email: String, password: String) derives JsonCodec, Schema
