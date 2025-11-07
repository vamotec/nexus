package app.mosia.nexus.domain.model.training

import app.mosia.nexus.domain.model.common.EntityId

import java.util.UUID

case class ModelId(value: UUID) extends EntityId[ModelId]
