package app.mosia.nexus
package application.dto.model.scene

import caliban.schema.{Schema as Cs, ArgBuilder}
import sttp.tapir.Schema
import zio.json.*
import zio.*

case class SensorSummary(
  sensorType: String,
  name: String
) derives JsonCodec,
      Schema
