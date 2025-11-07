package app.mosia.nexus.domain.model.scene

import app.mosia.nexus.domain.model.common.ValueObject

case class LightingConfig(ambientIntensity: Double, sunIntensity: Double, colorTemperature: Double, shadows: Boolean)
    extends ValueObject
