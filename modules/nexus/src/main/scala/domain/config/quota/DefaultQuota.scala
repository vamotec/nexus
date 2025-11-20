package app.mosia.nexus
package domain.config.quota

/** 默认配额限制 */
case class DefaultQuota(
  maxConcurrentSessions: Int,
  maxGpuHoursPerMonth: Double,
  maxStorageGb: Double
)
