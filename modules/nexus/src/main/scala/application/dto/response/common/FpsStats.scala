package app.mosia.nexus
package application.dto.response.common

import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

case class FpsStats(
  avg: Double,
  max: Double,
  min: Double,
  p50: Option[Double] = None,
  p99: Option[Double] = None
) derives Cs.SemiAuto,
      ArgBuilder
