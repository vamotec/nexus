package app.mosia.nexus
package application.dto.model.scene

import caliban.schema.{Schema as Cs, ArgBuilder}
import sttp.tapir.Schema
import zio.json.*
import zio.*

case class MaterialDto(
  name: String,
  friction: Double,
  restitution: Double,
  color: Option[String] = None // HEX 颜色
) derives JsonCodec,
      Schema,
      Cs.SemiAuto,
      ArgBuilder
