package app.mosia.nexus
package application.dto.response.training

import zio.json.*

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
) derives JsonCodec
