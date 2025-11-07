package app.mosia.nexus.application.dto.model.scene

import sttp.tapir.Schema
import zio.json.JsonCodec

case class SensorSummary(
  sensorType: String,
  name: String
) derives JsonCodec,
      Schema
