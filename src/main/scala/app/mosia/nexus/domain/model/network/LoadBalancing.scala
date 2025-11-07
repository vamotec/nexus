package app.mosia.nexus.domain.model.network

import zio.Duration

/** 负载均衡配置 */
case class LoadBalancing(
  algorithm: LoadBalanceAlgorithm,
  healthCheck: HealthCheckConfig,
  stickySessions: Boolean = false,
  drainTime: Duration = Duration.fromSeconds(30)
)
