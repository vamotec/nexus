package app.mosia.nexus
package domain.model.resource

import domain.model.common.{Bandwidth, MemorySize}
import domain.model.storage.{MountPointConfig, StorageType}

/** 存储需求 */
case class StorageRequirements(
  diskSpace: MemorySize,
  iops: Int, // IOPS 要求
  throughput: Bandwidth, // 吞吐量要求
  storageType: StorageType = StorageType.SSD,
  persistent: Boolean = false, // 是否需要持久化存储
  mountPoints: List[MountPointConfig] = List.empty
)
