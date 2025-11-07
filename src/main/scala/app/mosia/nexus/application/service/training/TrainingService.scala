package app.mosia.nexus.application.service.training

import app.mosia.nexus.application.dto.request.training.CreateTrainingRequest
import zio.*
import app.mosia.nexus.application.dto.response.training.{TrainingJobResponse, TrainingProgressResponse}
import app.mosia.nexus.domain.model.training.TrainingJobId
import app.mosia.nexus.domain.model.user.UserId

trait TrainingService:
  def createTrainingJob(userId: UserId, input: CreateTrainingRequest): ZIO[Any, Throwable | String, TrainingJobResponse]
  def getTrainingProgress(jobId: TrainingJobId): Task[TrainingProgressResponse]
  def stopTrainingJob(jobId: TrainingJobId): Task[TrainingJobResponse]
