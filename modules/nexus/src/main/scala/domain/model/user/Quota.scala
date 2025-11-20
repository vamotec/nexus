package app.mosia.nexus
package domain.model.user

import java.time.Instant
import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

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
