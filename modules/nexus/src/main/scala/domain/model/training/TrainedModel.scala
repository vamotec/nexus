package app.mosia.nexus
package domain.model.training

import domain.model.project.ProjectId

import java.time.Instant

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
