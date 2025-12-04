package app.mosia.nexus
package infrastructure.cloud

import domain.config.AppConfig
import domain.error.*
import domain.grpc.nebula.*
import domain.grpc.neuro.{HealthCheckRequest, NeuroOrchestratorServiceGrpc}
import domain.model.grpc.{ChannelPool, ClusterTarget, PoolHealth}

import io.grpc.ManagedChannel
import zio.*

final class ConnectionManager private (
  private val pools: Map[String, ChannelPool], // ← 不可变 Map
  config: AppConfig
):
  /** 获取指定 cluster 的 stub */
  def getNeuroStub(clusterId: String): AppTask[NeuroOrchestratorServiceGrpc.NeuroOrchestratorServiceStub] =
    ZIO
      .attempt {
        val pool = pools.getOrElse(
          clusterId,
          throw GrpcServiceError("neuro", "pool manager", io.grpc.Status.ABORTED)
        )
        val channel = pool.pick()
        NeuroOrchestratorServiceGrpc.stub(channel)
      }
      .mapError(toAppError)

  /** 获取指定 cluster 的NebulaFileService的 stub */
  def getNebulaFileStub(clusterId: String): AppTask[NebulaFileServiceGrpc.NebulaFileServiceStub] =
    ZIO
      .attempt {
        val pool = pools.getOrElse(
          clusterId,
          throw GrpcServiceError("nebula", "pool manager", io.grpc.Status.ABORTED)
        )
        val channel = pool.pick()
        NebulaFileServiceGrpc.stub(channel)
      }
      .mapError(toAppError)

  /** 获取指定 cluster 的NebulaCollabService的 stub */
  def getNebulaCollabStub(clusterId: String): AppTask[NebulaCollabServiceGrpc.NebulaCollabServiceStub] =
    ZIO
      .attempt {
        val pool = pools.getOrElse(
          clusterId,
          throw GrpcServiceError("nebula", "pool manager", io.grpc.Status.ABORTED)
        )
        val channel = pool.pick()
        NebulaCollabServiceGrpc.stub(channel)
      }
      .mapError(toAppError)

  /** 获取指定 cluster 的NebulaSyncService的 stub */
  def getNebulaSyncStub(clusterId: String): AppTask[NebulaSyncServiceGrpc.NebulaSyncServiceStub] =
    ZIO
      .attempt {
        val pool = pools.getOrElse(
          clusterId,
          throw GrpcServiceError("nebula", "pool manager", io.grpc.Status.ABORTED)
        )
        val channel = pool.pick()
        NebulaSyncServiceGrpc.stub(channel)
      }
      .mapError(toAppError)

  /** 获取指定 cluster 的NebulaNucleusService的 stub */
  def getNebulaNucleusStub(clusterId: String): AppTask[NebulaNucleusServiceGrpc.NebulaNucleusServiceStub] =
    ZIO
      .attempt {
        val pool = pools.getOrElse(
          clusterId,
          throw GrpcServiceError("nebula", "pool manager", io.grpc.Status.ABORTED)
        )
        val channel = pool.pick()
        NebulaNucleusServiceGrpc.stub(channel)
      }
      .mapError(toAppError)
    
  
  /** 测试单个 channel 健康 */
  private def checkChannel(channel: ManagedChannel): ZIO[Any, Throwable, Boolean] =
    val request = HealthCheckRequest()
    val stub    = NeuroOrchestratorServiceGrpc.stub(channel)
    ZIO
      .fromFuture(_ => stub.healthCheck(request))
      .as(true)
      .catchAll(_ => ZIO.succeed(false))

  /** 健康检查所有 pool */
  def healthCheck: UIO[Map[String, PoolHealth]] =
    ZIO
      .foreach(pools) { case (clusterId, pool) =>
        ZIO
          .foreach((0 until pool.size).toList) { idx =>
            val channel = pool.channels.get(idx)
            checkChannel(channel).map(idx -> _)
          }
          .map { results =>
            val healthy = results.count(_._2)
            clusterId -> PoolHealth(
              total = pool.size,
              healthy = healthy,
              unhealthy = pool.size - healthy
            )
          }
      }
      .map(_.toMap)
      .orElseSucceed(Map.empty)

object ConnectionManager:
  /** 创建 manager 的 ZLayer（scoped） */
  def make(target: ClusterTarget): ZLayer[AppConfig, AppError, ConnectionManager] =
    ZLayer.scoped {
      for {
        config <- ZIO.service[AppConfig]

        // 预创建所有 cluster 的 pool
        poolList <- ZIO
          .foreachPar(config.cloud.clusters.toList) { case (clusterId, clusterCfg) =>
            ChannelPool
              .make(clusterCfg, target)
              .map(clusterId -> _)
              .tapError(e => ZIO.logError(s"Failed to create pool for cluster [$clusterId]: ${e.getMessage}"))
          }
          .mapError(toAppError)

        pools = poolList.toMap
        _ <- ZIO.logInfo(s"Initialized ${pools.size} neuro(& nebula) cluster pools")

        manager = ConnectionManager(pools, config)

        // 使用全局配置的间隔
        _ <- healthCheckFiber(
          manager,
          config.cloud.healthCheckInterval
        ).forkScoped

      } yield manager
    }

  /** 单个 cluster 的健康检查 fiber */
  private def healthCheckFiber(
                                manager: ConnectionManager,
                                interval: Duration
                              ): ZIO[Any, Nothing, Unit] =
    val loop = for {
      _ <- ZIO.foreachDiscard(manager.pools) { case (clusterId, pool) =>
        ZIO.foreachDiscard((0 until pool.size).toList) { idx =>
          val channel = pool.channels.get(idx)
          manager.checkChannel(channel).flatMap { healthy =>
            if !healthy then
              ZIO.logWarning(
                s"Neuro Cluster [$clusterId] channel [$idx] unhealthy"
              )
            else ZIO.unit
          }
        }
      }
      _ <- ZIO.sleep(interval)
    } yield ()

    loop.forever
      .interruptible // 确保可以被中断
      .catchAllCause(c =>
        if c.isInterrupted then
          ZIO.logInfo("Health check fiber interrupted gracefully")
        else
          ZIO.logErrorCause("Health check fiber failed", c)
      )
//      .ensuring(
//        ZIO.logInfo("Health check fiber cleanup completed")
//      )
