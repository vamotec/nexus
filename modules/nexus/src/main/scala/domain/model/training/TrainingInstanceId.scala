package app.mosia.nexus
package domain.model.training

import domain.error.AppTask
import domain.model.common.EntityId

import java.util.UUID

case class TrainingInstanceId(value: UUID) extends EntityId[TrainingInstanceId]

object TrainingInstanceId:
  def generate(): TrainingInstanceId = TrainingInstanceId(UUID.randomUUID())

  def fromString(str: String): AppTask[TrainingInstanceId] =
    EntityId.fromString(str)(using TrainingInstanceId.apply)
