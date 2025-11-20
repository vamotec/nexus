package app.mosia.nexus
package domain.config.neuro

import zio.*

case class NeuroConfig(
  healthCheckInterval: Duration = 30.seconds,
  clusters: Map[String, ClustersConfig]
)
