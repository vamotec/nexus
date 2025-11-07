package app.mosia.nexus.domain.model.training

import java.time.Instant

import app.mosia.nexus.domain.model.project.ProjectId

case class TrainedModel(
  id: ModelId,
  projectId: ProjectId,
  trainingJobId: TrainingJobId,
  name: String,
  version: String,
  modelPath: String,
  algorithm: RLAlgorithm,
  metrics: Map[String, Double],
  createdAt: Instant
)
