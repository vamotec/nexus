package app.mosia.nexus.domain.model.agent

import app.mosia.nexus.domain.model.common.EntityId

import java.util.UUID

case class AgentId(value: UUID) extends EntityId[AgentId]
