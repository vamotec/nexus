package app.mosia.nexus.domain.model.user

import app.mosia.nexus.infra.persistence.postgres.entity.{UserEntity, UserQuotaEntity}
import org.mindrot.jbcrypt.BCrypt
import zio.json.JsonCodec

import java.time.Instant

case class User(
  id: UserId,
  email: String,
  name: String,
  avatar: String,
  organization: Option[String],
  role: UserRole,
  isActive: Boolean,
  emailVerified: Boolean,
  quota: Quota,
  createdAt: Instant,
  updatedAt: Instant,
  lastLoginAt: Option[Instant]
) derives JsonCodec:

  def canCreateSession: Boolean = isActive && quota.hasAvailableSessions

object User:
  def fromEntities(u: UserEntity, q: UserQuotaEntity): User =
    User(
      id = UserId(u.id),
      email = u.email,
      name = u.name,
      avatar = u.avatar,
      organization = u.organization,
      role = u.role,
      isActive = u.isActive,
      emailVerified = u.emailVerified,
      quota = Quota(
        maxConcurrentSessions = q.maxConcurrentSessions,
        maxGpuHoursPerMonth = q.maxGpuHoursPerMonth,
        maxStorageGb = q.maxStorageGb,
        currentActiveSessions = q.currentActiveSessions,
        currentGpuHoursThisMonth = q.currentGpuHoursThisMonth,
        currentStorageGb = q.currentStorageGb,
        quotaResetAt = q.quotaResetAt
      ),
      createdAt = u.createdAt,
      updatedAt = u.updatedAt,
      lastLoginAt = u.lastLoginAt
    )
