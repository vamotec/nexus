package app.mosia.nexus
package domain.model.session

import domain.model.common.ValueObject
import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

case class SessionError(
  code: String,
  message: String
) extends ValueObject derives JsonCodec
