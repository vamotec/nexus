package app.mosia.nexus.domain.model.user

import zio.json.JsonCodec

import java.time.Instant

case class Quota(
  maxConcurrentSessions: Int,
  maxGpuHoursPerMonth: Double,
  maxStorageGb: Double,
  currentActiveSessions: Int,
  currentGpuHoursThisMonth: Double,
  currentStorageGb: Double,
  quotaResetAt: Instant
) derives JsonCodec:
  def hasAvailableSessions: Boolean = currentActiveSessions < maxConcurrentSessions

  def remainingGpuHours: Double = maxGpuHoursPerMonth - currentGpuHoursThisMonth
