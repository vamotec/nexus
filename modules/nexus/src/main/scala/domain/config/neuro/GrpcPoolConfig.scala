package app.mosia.nexus
package domain.config.neuro

import zio.*

case class GrpcPoolConfig(
  connection: GrpcConnectionConfig,
  // 连接池特定配置
  poolSize: Int = 4,
  maxConcurrentRequests: Int = 100,
  acquireTimeout: Duration = 30.seconds,
  healthCheckInterval: Duration = 1.minute,
  maxWaiters: Int = 50
)
