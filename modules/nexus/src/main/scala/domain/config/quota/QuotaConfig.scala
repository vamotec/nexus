package app.mosia.nexus
package domain.config.quota

import zio.Duration

/** 资源配额配置 */
case class QuotaConfig(
  default: DefaultQuota,
  checkInterval: Duration,
  onExceeded: String // reject, queue, throttle
)
