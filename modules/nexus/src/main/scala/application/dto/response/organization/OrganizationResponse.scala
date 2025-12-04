package app.mosia.nexus
package application.dto.response.organization

import domain.model.organization.{Organization, OrganizationId, PlanType}

import java.time.Instant
import sttp.tapir.Schema
import zio.json.*

case class OrganizationResponse(
  id: String,
  name: String,
  description: Option[String],
  avatar: Option[String],
  planType: String,
  maxUsers: Int,
  maxStorageGb: Double,
  maxGpuHoursPerMonth: Double,
  isActive: Boolean,
  createdAt: Instant,
  updatedAt: Instant
) derives JsonCodec, Schema

object OrganizationResponse:
  def fromDomain(org: Organization): OrganizationResponse =
    OrganizationResponse(
      id = org.id.value.toString,
      name = org.name,
      description = org.description,
      avatar = org.avatar,
      planType = org.planType.toString.toLowerCase,
      maxUsers = org.quota.maxUsers,
      maxStorageGb = org.quota.maxStorageGb,
      maxGpuHoursPerMonth = org.quota.maxGpuHoursPerMonth,
      isActive = org.isActive,
      createdAt = org.createdAt,
      updatedAt = org.updatedAt
    )
