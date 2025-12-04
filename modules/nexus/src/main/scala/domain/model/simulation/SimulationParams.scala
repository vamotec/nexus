package app.mosia.nexus
package domain.model.simulation

import domain.error.*
import domain.model.common.{PhysicsEngine, ValueObject}
import domain.model.scene.Environment

import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

case class SimulationParams(
  physicsEngine: PhysicsEngine,
  timeStep: Double,
  maxDuration: Int = 10000, // 秒
  realTime: Boolean = true,
  recordVideo: Boolean = true,
  recordTrajectory: Boolean = true
) extends ValueObject derives JsonCodec, Schema
