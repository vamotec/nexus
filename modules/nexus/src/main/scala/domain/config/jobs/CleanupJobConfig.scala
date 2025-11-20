package app.mosia.nexus
package domain.config.jobs

import zio.Duration

/** 清理任务配置 */
case class CleanupJobConfig(
  enabled: Boolean,
  interval: Duration,
  retentionDays: Int
)
