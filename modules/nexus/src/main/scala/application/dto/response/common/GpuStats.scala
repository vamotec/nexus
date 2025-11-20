package app.mosia.nexus
package application.dto.response.common

import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

case class GpuStats(
  avgUtilization: Double,
  maxUtilization: Option[Double] = None,
  maxMemoryMb: Long
) derives Cs.SemiAuto,
      ArgBuilder
