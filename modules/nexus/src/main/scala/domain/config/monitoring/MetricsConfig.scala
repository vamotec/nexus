package app.mosia.nexus
package domain.config.monitoring

import zio.Duration

/** 指标收集配置 */
case class MetricsConfig(
  sessionSyncInterval: Duration,
  trainingUpdateInterval: Duration,
  collectSystemMetrics: Boolean
)
