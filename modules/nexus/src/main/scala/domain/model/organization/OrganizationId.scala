package app.mosia.nexus
package domain.model.organization

import domain.model.common.EntityId

import app.mosia.nexus.domain.error.AppTask
import zio.json.*

import java.util.UUID

case class OrganizationId(value: UUID) extends EntityId[OrganizationId] derives JsonCodec

object OrganizationId:
  def generate(): OrganizationId = OrganizationId(UUID.randomUUID())

  def fromString(str: String): AppTask[OrganizationId] =
    EntityId.fromString(str)(using OrganizationId.apply)
