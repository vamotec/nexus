package app.mosia.nexus.domain.model.training

import zio.json.JsonCodec

import java.util.UUID

import app.mosia.nexus.domain.model.common.EntityId

case class TrainingJobId(value: UUID) extends EntityId[TrainingJobId] derives JsonCodec

object TrainingJobId:
  def generate(): TrainingJobId = TrainingJobId(UUID.randomUUID())
