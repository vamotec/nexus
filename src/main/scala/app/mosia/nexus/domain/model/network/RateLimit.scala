package app.mosia.nexus.domain.model.network

/** 速率限制 */
case class RateLimit(
  requestsPerSecond: Int,
  burstSize: Int,
  by: RateLimitBy = RateLimitBy.IP
)
