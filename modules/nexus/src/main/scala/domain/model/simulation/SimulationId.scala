package app.mosia.nexus
package domain.model.simulation

import domain.error.AppTask
import domain.model.common.EntityId

import java.util.UUID

import zio.json.*

case class SimulationId(value: UUID) extends EntityId[SimulationId] derives JsonCodec

object SimulationId:
  def generate(): SimulationId = SimulationId(UUID.randomUUID())

  def fromString(str: String): AppTask[SimulationId] =
    EntityId.fromString(str)(using SimulationId.apply)
