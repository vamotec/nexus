package app.mosia.nexus
package application.dto.request.user

import sttp.tapir.Schema
import zio.json.*

case class UserCreateRequest(email: String, password: String) derives JsonCodec, Schema
