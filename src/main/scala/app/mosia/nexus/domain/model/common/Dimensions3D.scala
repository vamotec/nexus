package app.mosia.nexus.domain.model.common

import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.JsonCodec

case class Dimensions3D(width: Double, height: Double, depth: Double) extends ValueObject
    derives JsonCodec,
      Schema,
      Cs.SemiAuto,
      ArgBuilder
