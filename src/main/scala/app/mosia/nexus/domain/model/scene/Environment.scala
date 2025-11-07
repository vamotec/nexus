package app.mosia.nexus.domain.model.scene

import app.mosia.nexus.domain.model.common.ValueObject

case class Environment(environmentType: EnvironmentType, lighting: LightingConfig, physics: PhysicsConfig)
    extends ValueObject
