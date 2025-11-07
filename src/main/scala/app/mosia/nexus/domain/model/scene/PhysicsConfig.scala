package app.mosia.nexus.domain.model.scene

import app.mosia.nexus.domain.model.common.ValueObject

case class PhysicsConfig(gravity: Double, timeStep: Double, subSteps: Int, solverIterations: Int) extends ValueObject
