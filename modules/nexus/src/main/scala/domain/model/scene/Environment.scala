package app.mosia.nexus
package domain.model.scene

import zio.json.*

case class Environment(environmentType: EnvironmentType, lighting: LightingConfig, physics: PhysicsConfig)
    derives JsonCodec
