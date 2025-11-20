package app.mosia.nexus
package domain.model.resource

import domain.model.common.MemorySize

/** 计算需求 */
case class ComputeRequirements(
  computeType: ComputeType,
  minCpuCores: Int,
  maxCpuCores: Option[Int] = None,
  minGpuCount: Int = 0,
  maxGpuCount: Option[Int] = None,
  gpuMemoryPerCard: Option[MemorySize] = None, // 每张GPU显存
  minTpuCount: Int = 0,
  cpuArchitecture: String = "x86_64",
  instructionSet: Set[String] = Set("AVX2")
)
