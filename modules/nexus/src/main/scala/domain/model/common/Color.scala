package app.mosia.nexus
package domain.model.common

import zio.json.*
import zio.*

case class Color(r: Double, g: Double, b: Double, a: Double = 1.0) extends ValueObject derives JsonCodec

object Color:
  val Red   = Color(1, 0, 0)
  val Green = Color(0, 1, 0)
  val Blue  = Color(0, 0, 1)
  val White = Color(1, 1, 1)
  val Black = Color(0, 0, 0)
