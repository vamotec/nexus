package app.mosia.nexus
package domain.model.resource

import domain.model.common.ValueObject

import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

case class StreamEndpoint(host: String, port: Int, protocol: String) extends ValueObject derives JsonCodec
