package app.mosia.nexus
package application.dto.model.scene

import caliban.schema.{Schema as Cs, ArgBuilder}
import sttp.tapir.Schema
import zio.json.*
import zio.*

case class SceneConfigSummary(
  name: String,
  robotType: String,
  environment: String,
  hasGoal: Boolean,
  sensorCount: Int
) derives JsonCodec,
      Schema
