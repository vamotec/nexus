package app.mosia.nexus
package domain.model.resource

import domain.model.common.{Bandwidth, MemorySize}

/** 内存需求 */
case class MemoryRequirements(
  minRam: MemorySize,
  maxRam: Option[MemorySize] = None,
  sharedMemory: Option[MemorySize] = None, // 共享内存
  swapRequired: Boolean = false,
  memoryBandwidth: Option[Bandwidth] = None // 内存带宽要求
)
