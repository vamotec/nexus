package app.mosia.nexus.domain.model.storage

import app.mosia.nexus.domain.model.common.Bandwidth

/** 存储性能等级 */
case class StoragePerformance(
  iops: Range, // IOPS 范围
  throughput: Bandwidth, // 吞吐量
  latency: LatencyProfile, // 延迟特性
  durability: Double = 0.9999999999 // 耐久性 (11个9)
)
