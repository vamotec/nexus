package app.mosia.nexus.domain.model.simulation

import app.mosia.nexus.domain.model.common.EntityId
import caliban.schema.{ArgBuilder, Schema}
import zio.json.JsonCodec

import java.util.UUID

case class SimulationId(value: UUID) extends EntityId[SimulationId] derives JsonCodec, Schema.SemiAuto, ArgBuilder
