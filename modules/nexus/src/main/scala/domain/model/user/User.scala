package app.mosia.nexus
package domain.model.user

import domain.model.organization.OrganizationId

import java.time.Instant
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

case class User(
  id: UserId,
  email: String,
  name: String,
  avatar: Option[String],
  role: UserRole,
  isActive: Boolean,
  emailVerified: Boolean,
  quota: Quota,
  createdAt: Instant,
  updatedAt: Instant,
  lastLoginAt: Option[Instant]
) derives JsonCodec:

  def canCreateSession: Boolean = isActive && quota.hasAvailableSessions
