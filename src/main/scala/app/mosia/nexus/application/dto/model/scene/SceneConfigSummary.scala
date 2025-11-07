package app.mosia.nexus.application.dto.model.scene

import sttp.tapir.Schema
import zio.json.JsonCodec

case class SceneConfigSummary(
  name: String,
  robotType: String,
  environment: String,
  hasGoal: Boolean,
  sensorCount: Int
) derives JsonCodec,
      Schema
