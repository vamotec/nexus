package app.mosia.nexus
package domain.model.scene

import domain.model.common.ValueObject
import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

case class PhysicsConfig(gravity: Double, timeStep: Double, subSteps: Int, solverIterations: Int) extends ValueObject
    derives JsonCodec
