package app.mosia.nexus
package domain.model.training

import domain.model.common.EntityId

import java.util.UUID

case class ModelId(value: UUID) extends EntityId[ModelId]
