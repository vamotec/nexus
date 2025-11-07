package app.mosia.nexus.application.dto.model.scene

import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.JsonCodec

case class MaterialDto(
  name: String,
  friction: Double,
  restitution: Double,
  color: Option[String] = None // HEX 颜色
) derives JsonCodec,
      Schema,
      Cs.SemiAuto,
      ArgBuilder
