package app.mosia.nexus
package domain.model.training

import domain.error.AppTask
import domain.model.common.EntityId

import java.util.UUID

import zio.json.*

case class TrainingJobId(value: UUID) extends EntityId[TrainingJobId] derives JsonCodec

object TrainingJobId:
  def generate(): TrainingJobId = TrainingJobId(UUID.randomUUID())

  def fromString(str: String): AppTask[TrainingJobId] =
    EntityId.fromString(str)(using TrainingJobId.apply)
