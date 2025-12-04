package app.mosia.nexus
package domain.model.user

import domain.model.common.ValueObject

import zio.json.*

case class ResourceQuota(
  maxConcurrentSessions: Int,
  maxGpuHoursPerMonth: Double,
  maxStorageGB: Double,
  currentUsage: ResourceUsage
) extends ValueObject derives JsonCodec
