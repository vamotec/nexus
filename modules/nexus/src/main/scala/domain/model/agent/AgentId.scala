package app.mosia.nexus
package domain.model.agent

import domain.model.common.EntityId

import java.util.UUID

case class AgentId(value: UUID) extends EntityId[AgentId]
