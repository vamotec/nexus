package app.mosia.nexus.application.dto.response.training

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
)
