package app.mosia.nexus
package infrastructure.cloud

import domain.config.AppConfig
import domain.model.grpc.ClusterMetadata
import domain.services.infra.ClusterRegistry

import zio.*

final class ClusterRegistryLive(config: AppConfig) extends ClusterRegistry:

  private val metadataMap: Map[String, ClusterMetadata] =
    initializeMetadata(config)

  override def getHealthyClusters: UIO[List[ClusterMetadata]] =
    ZIO
      .foreach(metadataMap.values.toList) { metadata =>
        metadata.healthy.get.map { isHealthy =>
          if isHealthy then Some(metadata) else None
        }
      }
      .map(_.flatten)

  override def isClusterHealthy(clusterId: String): UIO[Boolean] =
    metadataMap.get(clusterId) match {
      case Some(metadata) => metadata.healthy.get
      case None => ZIO.succeed(false)
    }

  override def updateLoad(clusterId: String, delta: Int): UIO[Unit] =
    metadataMap.get(clusterId) match {
      case Some(metadata) =>
        metadata.currentLoad.update(_ + delta).unit
      case None =>
        ZIO.unit
    }

  override def getClusterMetadata(clusterId: String): UIO[Option[ClusterMetadata]] =
    ZIO.succeed(metadataMap.get(clusterId))

  private def initializeMetadata(config: AppConfig): Map[String, ClusterMetadata] =
    config.cloud.clusters.map { case (clusterId, clusterCfg) =>
      clusterId -> ClusterMetadata(
        id = clusterId,
        location = clusterCfg.location, // 需要在配置中添加
        capacity = clusterCfg.capacity,
        currentLoad = Unsafe.unsafe { implicit unsafe =>
          Runtime.default.unsafe.run(Ref.make(0)).getOrThrow()
        },
        healthy = Unsafe.unsafe { implicit unsafe =>
          Runtime.default.unsafe.run(Ref.make(true)).getOrThrow()
        },
        priority = clusterCfg.priority,
        tags = clusterCfg.tags
      )
    }

object ClusterRegistryLive:
  val live: ZLayer[AppConfig, Nothing, ClusterRegistry] =
    ZLayer.fromFunction(ClusterRegistryLive(_))
