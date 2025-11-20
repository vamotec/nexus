package app.mosia.nexus
package domain.model.organization

import domain.model.common.EntityId

import zio.json.*

import java.util.UUID

case class OrganizationId(value: UUID) extends EntityId[OrganizationId] derives JsonCodec
