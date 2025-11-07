package app.mosia.nexus.domain.model.common

import app.mosia.nexus.domain.model.common.EntityId

import java.util.UUID

case class TemplateId(value: UUID) extends EntityId[TemplateId]
