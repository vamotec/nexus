package app.mosia.nexus
package application.dto.response.organization

import domain.model.organization.OrganizationMember

import java.time.Instant
import sttp.tapir.Schema
import zio.json.*

case class OrganizationMemberResponse(
  organizationId: String,
  userId: String,
  role: String,
  isActive: Boolean,
  isInvited: Boolean,
  joinedAt: Instant,
  leftAt: Option[Instant],
  invitedBy: Option[String],
  invitedAt: Option[Instant]
) derives JsonCodec, Schema

object OrganizationMemberResponse:
  def fromDomain(member: OrganizationMember): OrganizationMemberResponse =
    OrganizationMemberResponse(
      organizationId = member.organizationId.value.toString,
      userId = member.userId.value.toString,
      role = member.role.toString.toLowerCase,
      isActive = member.isActive,
      isInvited = member.isInvited,
      joinedAt = member.joinedAt,
      leftAt = member.leftAt,
      invitedBy = member.invitedBy.map(_.value.toString),
      invitedAt = member.invitedAt
    )
