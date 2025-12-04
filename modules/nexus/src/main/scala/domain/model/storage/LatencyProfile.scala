package app.mosia.nexus
package domain.model.storage

import zio.*

/** 延迟特性 */
case class LatencyProfile(
  readLatency: Duration,
  writeLatency: Duration,
  averageLatency: Duration,
  p95Latency: Duration,
  p99Latency: Duration
)
