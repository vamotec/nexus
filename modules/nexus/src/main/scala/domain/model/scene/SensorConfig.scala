package app.mosia.nexus
package domain.model.scene

import domain.model.common.ValueObject
import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

case class SensorConfig(
  resolution: Option[(Int, Int)],
  frequency: Option[Double],
  range: Option[Double],
  fov: Option[Double] // Field of view
) derives JsonCodec
