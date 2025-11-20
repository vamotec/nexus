package app.mosia.nexus
package domain.model.network

import zio.*

/** 健康检查配置 */
case class HealthCheckConfig(
  path: String = "/health",
  interval: Duration = Duration.fromSeconds(30),
  timeout: Duration = Duration.fromSeconds(5),
  healthyThreshold: Int = 2,
  unhealthyThreshold: Int = 3
)
