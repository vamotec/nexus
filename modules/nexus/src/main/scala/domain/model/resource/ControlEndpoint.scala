package app.mosia.nexus
package domain.model.resource

import domain.model.common.ValueObject

import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

case class ControlEndpoint(wsUrl: String) extends ValueObject derives JsonCodec
