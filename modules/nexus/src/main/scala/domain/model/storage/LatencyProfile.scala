package app.mosia.nexus
package domain.model.storage

import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

/** 延迟特性 */
case class LatencyProfile(
  readLatency: Duration,
  writeLatency: Duration,
  averageLatency: Duration,
  p95Latency: Duration,
  p99Latency: Duration
)
