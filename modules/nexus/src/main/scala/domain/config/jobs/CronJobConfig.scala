package app.mosia.nexus
package domain.config.jobs

import zio.Duration

/** Cron 任务配置 */
case class CronJobConfig(
  enabled: Boolean,
  interval: Duration,
  cron: String
)
