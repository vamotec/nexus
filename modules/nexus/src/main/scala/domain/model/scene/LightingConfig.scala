package app.mosia.nexus
package domain.model.scene

import domain.model.common.ValueObject
import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

case class LightingConfig(ambientIntensity: Double, sunIntensity: Double, colorTemperature: Double, shadows: Boolean)
    extends ValueObject derives JsonCodec
