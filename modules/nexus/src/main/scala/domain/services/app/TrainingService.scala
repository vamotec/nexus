package app.mosia.nexus
package domain.services.app

import application.dto.request.training.CreateTrainingRequest
import application.dto.response.training.{TrainingJobResponse, TrainingProgressResponse}
import domain.error.AppTask
import domain.model.training.TrainingJobId
import domain.model.user.UserId
import zio.json.*
import zio.*
import zio.http.*

trait TrainingService:
  def createTrainingJob(userId: UserId, input: CreateTrainingRequest): AppTask[TrainingJobResponse]
  def getTrainingJob(jobId: TrainingJobId): AppTask[Option[TrainingJobResponse]]
  def getMyTrainingJobs(userId: UserId, limit: Int): AppTask[List[TrainingJobResponse]]
  def getTrainingProgress(jobId: TrainingJobId): AppTask[TrainingProgressResponse]
  def stopTrainingJob(jobId: TrainingJobId): AppTask[TrainingJobResponse]
