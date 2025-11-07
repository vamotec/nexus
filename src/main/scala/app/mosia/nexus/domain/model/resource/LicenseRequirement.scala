package app.mosia.nexus.domain.model.resource

/** 许可需求 */
case class LicenseRequirement(
  product: String,
  version: String,
  floating: Boolean = false, // 浮动许可
  concurrentUsers: Int = 1
)
