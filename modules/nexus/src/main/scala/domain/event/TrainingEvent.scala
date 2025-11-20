package app.mosia.nexus
package domain.event

import domain.model.session.SessionId
import domain.model.training.{TrainingJobId, TrainingResult}

import zio.json.*

import java.time.Instant
import java.util.UUID

@jsonDiscriminator("type")
enum TrainingEvent derives JsonCodec:
  case TrainingJobCreated(jobId: TrainingJobId, sessionId: SessionId, occurredAt: Instant)
  case TrainingJobCompleted(jobId: TrainingJobId, result: TrainingResult, occurredAt: Instant)

  def aggregateId: UUID = this match
    case TrainingJobCreated(jobId, _, _) => jobId.value
    case TrainingJobCompleted(jobId, _, _) => jobId.value
