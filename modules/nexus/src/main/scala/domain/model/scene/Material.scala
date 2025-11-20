package app.mosia.nexus
package domain.model.scene

import domain.model.common.Color
import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

case class Material(
  name: String,
  color: Option[Color],
  texture: Option[String],
  friction: Double,
  restitution: Double
) derives JsonCodec
