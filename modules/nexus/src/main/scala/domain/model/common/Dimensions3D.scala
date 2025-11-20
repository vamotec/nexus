package app.mosia.nexus
package domain.model.common

import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

case class Dimensions3D(width: Double, height: Double, depth: Double) extends ValueObject
    derives JsonCodec,
      Schema,
      Cs.SemiAuto,
      ArgBuilder
