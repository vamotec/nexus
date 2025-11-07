package app.mosia.nexus.domain.event

import zio.json.{jsonDiscriminator, JsonCodec}

import java.time.Instant
import app.mosia.nexus.domain.model.session.SessionId
import app.mosia.nexus.domain.model.training.{TrainingJobId, TrainingResult}

import java.util.UUID

@jsonDiscriminator("type")
enum TrainingEvent derives JsonCodec:
  case TrainingJobCreated(jobId: TrainingJobId, sessionId: SessionId, occurredAt: Instant)
  case TrainingJobCompleted(jobId: TrainingJobId, result: TrainingResult, occurredAt: Instant)

  def aggregateId: UUID = this match
    case TrainingJobCreated(jobId, _, _) => jobId.value
    case TrainingJobCompleted(jobId, _, _) => jobId.value
