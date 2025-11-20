package app.mosia.nexus
package domain.model.simulation

import domain.error.AppTask
import domain.model.common.EntityId

import java.util.UUID

import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

case class SimulationId(value: UUID) extends EntityId[SimulationId] derives JsonCodec, Cs.SemiAuto, ArgBuilder

object SimulationId:
  def generate(): SimulationId = SimulationId(UUID.randomUUID())

  def fromString(str: String): AppTask[SimulationId] =
    EntityId.fromString(str)(using SimulationId.apply)
