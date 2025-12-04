package app.mosia.nexus
package infrastructure.cloud

import application.util.toZIOOrFail
import domain.config.cloud.GeoLocation
import domain.error.*
import domain.model.grpc.{ClusterMetadata, RoutingContext}
import domain.model.project.ProjectSettings
import domain.repository.ProjectRepository
import domain.services.infra.{ClusterRegistry, ClusterRoutingStrategy, RedisService}

import zio.{ZIO, ZLayer}

final class SmartRoutingStrategy(
  clusterRegistry: ClusterRegistry,
  redis: RedisService
) extends ClusterRoutingStrategy:

  override def selectCluster(context: RoutingContext): AppTask[String] =
    (for
      healthyClusters <- clusterRegistry.getHealthyClusters

      eligibleClusters = filterByProjectRequirements(
        healthyClusters,
        context.project.settings
      )

      _ <- ZIO.when(eligibleClusters.isEmpty)(
        ZIO.fail(NotFound("cluster", "none id"))
      )

      selected <- redis
        .get(context.userId.value.toString)
        .flatMap:
          case Some(clusterId) if eligibleClusters.exists(_.id == clusterId) =>
            // 粘性 cluster 存在且满足要求，直接使用
            ZIO.succeed(clusterId)

          case _ =>
            // 没有粘性或粘性 cluster 不可用，重新选择
            for {
              scored <- ZIO.foreach(eligibleClusters) { cluster =>
                scoreCluster(cluster, context).map(score => (cluster, score))
              }

              (selectedCluster, score) = scored.maxBy(_._2)

              _ <- ZIO.logDebug(
                s"Selected cluster [${selectedCluster.id}] with score [$score] " +
                  s"for user [${context.userId.value}]"
              )

            } yield selectedCluster.id

      // 4. 更新缓存
      _ <- redis.set(key = context.userId.value.toString, value = selected)
    yield selected).mapError(toAppError)

  // 计算 cluster 得分
  private def scoreCluster(
    cluster: ClusterMetadata,
    context: RoutingContext
  ): AppTask[Double] =
    for {
      currentLoad <- cluster.currentLoad.get

      // 地理位置得分
      geoScore = calculateGeoScore(
        context.userLocation,
        cluster.location
      )

      // 负载得分
      loadScore = 1.0 - (currentLoad.toDouble / cluster.capacity)

      // 项目偏好得分
      preferenceScore = calculatePreferenceScore(cluster, context.project.settings)

      // 综合得分（没有亲和性，因为是新选择）
      totalScore = geoScore * 0.5 + loadScore * 0.3 + preferenceScore * 0.2

    } yield totalScore

  // 根据项目需求过滤 cluster
  private def filterByProjectRequirements(
    clusters: List[ClusterMetadata],
    metadata: ProjectSettings
  ): List[ClusterMetadata] =
    clusters.filter { cluster =>
      // GPU 要求
      val hasGpu = !metadata.requiredGpu || cluster.tags.contains("gpu")

      // 内存要求
      val hasEnoughMemory = !cluster.tags.contains("memory-limited") ||
        metadata.requiredMemoryGb <= 16

      hasGpu && hasEnoughMemory
    }

  // 地理位置得分
  private def calculateGeoScore(
    userLocation: GeoLocation,
    clusterLocation: GeoLocation
  ): Double =
    val distance = calculateDistance(userLocation, clusterLocation)
    Math.exp(-distance / 5000.0)

  // 项目偏好得分
  private def calculatePreferenceScore(
    cluster: ClusterMetadata,
    metadata: ProjectSettings
  ): Double =
    val regionMatch = metadata.preferredRegions.exists(region => cluster.tags.contains(s"region-$region"))

    if regionMatch then 1.0
    else if metadata.preferredRegions.isEmpty then 0.5
    else 0.0

  private def calculateDistance(loc1: GeoLocation, loc2: GeoLocation): Double =
    val R    = 6371.0
    val lat1 = Math.toRadians(loc1.latitude)
    val lat2 = Math.toRadians(loc2.latitude)
    val dLat = Math.toRadians(loc2.latitude - loc1.latitude)
    val dLon = Math.toRadians(loc2.longitude - loc1.longitude)

    val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
      Math.cos(lat1) * Math.cos(lat2) *
      Math.sin(dLon / 2) * Math.sin(dLon / 2)
    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

    R * c

object SmartRoutingStrategy:
  val live: ZLayer[ClusterRegistry & RedisService, Nothing, ClusterRoutingStrategy] =
    ZLayer.fromFunction(SmartRoutingStrategy(_, _))
