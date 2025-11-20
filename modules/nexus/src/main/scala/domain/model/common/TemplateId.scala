package app.mosia.nexus
package domain.model.common

import domain.model.common.EntityId

import java.util.UUID

case class TemplateId(value: UUID) extends EntityId[TemplateId]
