package app.mosia.nexus
package application.dto.model.scene

import sttp.tapir.Schema
import zio.json.*
import zio.*

case class SensorSummary(
  sensorType: String,
  name: String
) derives JsonCodec,
      Schema
