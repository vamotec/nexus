package app.mosia.nexus.domain.model.simulation

import app.mosia.nexus.domain.model.common.{PhysicsEngine, ValueObject}
import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.JsonCodec

case class SimulationParams(
  physicsEngine: PhysicsEngine,
  timeStep: Double,
  maxDuration: Int, // 秒
  realTime: Boolean,
  recordVideo: Boolean,
  recordTrajectory: Boolean
) extends ValueObject derives JsonCodec, Schema, Cs.SemiAuto, ArgBuilder
