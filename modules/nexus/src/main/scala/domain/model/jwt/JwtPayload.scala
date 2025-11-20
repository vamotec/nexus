package app.mosia.nexus
package domain.model.jwt

import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

@jsonMemberNames(SnakeCase)
case class JwtPayload(userIdStr: String) derives JsonCodec
