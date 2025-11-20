package app.mosia.nexus
package domain.model.network

/** 速率限制 */
case class RateLimit(
  requestsPerSecond: Int,
  burstSize: Int,
  by: RateLimitBy = RateLimitBy.IP
)
