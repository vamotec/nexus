package app.mosia.nexus
package domain.config.jobs

import zio.Duration

/** 基础任务配置 */
case class JobConfig(
  enabled: Boolean,
  interval: Duration
)
