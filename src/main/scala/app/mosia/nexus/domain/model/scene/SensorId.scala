package app.mosia.nexus.domain.model.scene

import app.mosia.nexus.domain.model.common.EntityId

import java.util.UUID

case class SensorId(value: UUID) extends EntityId[SensorId]
