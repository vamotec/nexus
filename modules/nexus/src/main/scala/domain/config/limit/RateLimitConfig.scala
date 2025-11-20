package app.mosia.nexus
package domain.config.limit

/** 速率限流配置 */
case class RateLimitConfig(
  requestsPerSecond: Int,
  burstSize: Int
)
