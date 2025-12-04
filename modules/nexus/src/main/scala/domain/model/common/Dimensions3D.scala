package app.mosia.nexus
package domain.model.common

import sttp.tapir.Schema
import zio.json.*

case class Dimensions3D(width: Double, height: Double, depth: Double) extends ValueObject
    derives JsonCodec,
      Schema
