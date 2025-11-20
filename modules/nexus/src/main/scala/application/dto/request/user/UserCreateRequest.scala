package app.mosia.nexus
package application.dto.request.user

import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

case class UserCreateRequest(email: String, password: String) derives JsonCodec, Schema
