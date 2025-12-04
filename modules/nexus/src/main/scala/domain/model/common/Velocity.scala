package app.mosia.nexus
package domain.model.common

import zio.json.*
import zio.*
import zio.json.ast.Json

case class Velocity(linear: Double, angular: Double) extends ValueObject
