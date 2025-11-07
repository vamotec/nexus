package app.mosia.nexus.domain.model.network

import zio.Duration

/** 健康检查配置 */
case class HealthCheckConfig(
  path: String = "/health",
  interval: Duration = Duration.fromSeconds(30),
  timeout: Duration = Duration.fromSeconds(5),
  healthyThreshold: Int = 2,
  unhealthyThreshold: Int = 3
)
