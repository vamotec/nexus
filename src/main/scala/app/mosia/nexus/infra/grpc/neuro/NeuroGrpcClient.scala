package app.mosia.nexus.infra.grpc.neuro

import app.mosia.nexus.application.dto.request.training.CreateTrainingRequest
import zio.Task
import app.mosia.nexus.domain.model.training.TrainingProgress
import app.mosia.nexus.infra.grpc.neuro.mock.dto.*

trait NeuroGrpcClient:
  def createSession(request: GrpcCreateSessionRequest): Task[GrpcCreateSessionResponse]

  def startSimulation(sessionId: String): Task[Unit]

  def stopSimulation(sessionId: String, cleanup: Boolean): Task[Unit]

  def getSessionStatus(sessionId: String): Task[GrpcSessionStatusResponse]

  def startTraining(jobId: String, config: CreateTrainingRequest): Task[Unit]

  def getTrainingProgress(jobId: String): Task[TrainingProgress]

  def stopTraining(jobId: String): Task[Unit]
