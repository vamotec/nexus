package app.mosia.nexus
package domain.config.limit

/** 限流配置 */
case class LimitConfig(
  enabled: Boolean,
  perIp: RateLimitConfig,
  perUser: RateLimitConfig,
  endpoints: EndpointsLimitConfig
)
