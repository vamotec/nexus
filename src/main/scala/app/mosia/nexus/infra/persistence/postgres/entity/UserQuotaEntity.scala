package app.mosia.nexus.infra.persistence.postgres.entity

import java.time.Instant
import java.util.UUID

case class UserQuotaEntity(
  userId: UUID,
  // 配额限制
  maxConcurrentSessions: Int,
  maxGpuHoursPerMonth: Double,
  maxStorageGb: Double,
  // 当前使用量
  currentActiveSessions: Int,
  currentGpuHoursThisMonth: Double,
  currentStorageGb: Double,
  // 统计周期
  quotaResetAt: Instant,
  updatedAt: Instant
)
