package app.mosia.nexus.domain.model.resource

import app.mosia.nexus.domain.model.common.{Bandwidth, MemorySize}
import app.mosia.nexus.domain.model.storage.{MountPointConfig, StorageType}

/** 存储需求 */
case class StorageRequirements(
  diskSpace: MemorySize,
  iops: Int, // IOPS 要求
  throughput: Bandwidth, // 吞吐量要求
  storageType: StorageType = StorageType.SSD,
  persistent: Boolean = false, // 是否需要持久化存储
  mountPoints: List[MountPointConfig] = List.empty
)
