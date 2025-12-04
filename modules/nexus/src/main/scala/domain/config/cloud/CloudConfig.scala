package app.mosia.nexus
package domain.config.cloud

import zio.*

case class CloudConfig(
  healthCheckInterval: Duration = 30.seconds,
  clusters: Map[String, ClustersConfig]
)
