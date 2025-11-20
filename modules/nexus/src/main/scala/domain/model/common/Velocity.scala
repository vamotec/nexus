package app.mosia.nexus
package domain.model.common

import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

case class Velocity(linear: Double, angular: Double) extends ValueObject derives Cs.SemiAuto, ArgBuilder
