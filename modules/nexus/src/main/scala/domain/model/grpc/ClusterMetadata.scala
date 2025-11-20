package app.mosia.nexus
package domain.model.grpc

import domain.config.neuro.GeoLocation

import zio.json.*
import zio.*
import zio.json.ast.Json

final case class ClusterMetadata(
  id: String,
  location: GeoLocation,
  capacity: Int,
  currentLoad: Ref[Int], // 当前负载
  healthy: Ref[Boolean], // 健康状态
  priority: Int = 0, // 优先级（数字越大越优先）
  tags: Set[String] = Set.empty // 标签：如 "gpu", "high-memory"
)
