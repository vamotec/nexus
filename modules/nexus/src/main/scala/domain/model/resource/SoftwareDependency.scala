package app.mosia.nexus
package domain.model.resource

/** 软件依赖 */
case class SoftwareDependency(
  name: String,
  version: String,
  packageManager: Option[String] = None, // e.g., "pip", "conda", "apt"
  installScript: Option[String] = None
)
