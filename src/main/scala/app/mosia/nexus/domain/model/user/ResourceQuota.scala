package app.mosia.nexus.domain.model.user

import app.mosia.nexus.domain.model.common.ValueObject
import zio.json.JsonCodec

case class ResourceQuota(
  maxConcurrentSessions: Int,
  maxGpuHoursPerMonth: Double,
  maxStorageGB: Double,
  currentUsage: ResourceUsage
) extends ValueObject derives JsonCodec
