package app.mosia.nexus.domain.model.resource

/** 资源配额 */
case class ResourceRequirements(
  // 计算资源
  compute: ComputeRequirements,

  // 内存需求
  memory: MemoryRequirements,

  // 存储需求
  storage: StorageRequirements,

  // 网络需求
  network: NetworkRequirements,

  // 特殊硬件需求
  specialHardware: List[SpecialHardware],

  // 软件依赖
  softwareDependencies: List[SoftwareDependency],

  // 许可需求
  licenses: List[LicenseRequirement],

  // 服务质量要求
  qos: QualityOfService
)
