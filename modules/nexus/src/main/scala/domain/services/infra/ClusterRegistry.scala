package app.mosia.nexus
package domain.services.infra

import domain.model.grpc.ClusterMetadata
import zio.json.*
import zio.*

trait ClusterRegistry:
  def getHealthyClusters: UIO[List[ClusterMetadata]]

  def isClusterHealthy(clusterId: String): UIO[Boolean]

  def updateLoad(clusterId: String, delta: Int): UIO[Unit]

  def getClusterMetadata(clusterId: String): UIO[Option[ClusterMetadata]]
