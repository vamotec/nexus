package app.mosia.nexus.domain.model.storage

import zio.Duration

/** 延迟特性 */
case class LatencyProfile(
  readLatency: Duration,
  writeLatency: Duration,
  averageLatency: Duration,
  p95Latency: Duration,
  p99Latency: Duration
)
