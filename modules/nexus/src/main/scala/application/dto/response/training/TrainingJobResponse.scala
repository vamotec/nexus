package app.mosia.nexus
package application.dto.response.training

import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

case class TrainingJobResponse(
  id: String,
  sessionId: String,
  algorithm: String,
  status: String,
  progress: Option[TrainingProgressResponse],
  result: Option[TrainingResultResponse],
  createdAt: Long,
  startedAt: Option[Long],
  completedAt: Option[Long]
) derives JsonCodec,
      Cs.SemiAuto,
      ArgBuilder
