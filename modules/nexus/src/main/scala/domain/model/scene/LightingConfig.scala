package app.mosia.nexus
package domain.model.scene

import domain.model.common.ValueObject

import zio.json.*

case class LightingConfig(ambientIntensity: Double, sunIntensity: Double, colorTemperature: Double, shadows: Boolean)
    extends ValueObject derives JsonCodec
