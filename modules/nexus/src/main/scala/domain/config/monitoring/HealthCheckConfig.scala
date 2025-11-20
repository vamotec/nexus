package app.mosia.nexus
package domain.config.monitoring

import zio.Duration

/** 健康检查配置 */
case class HealthCheckConfig(
  enabled: Boolean,
  path: String,
  interval: Duration
)
