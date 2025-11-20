package app.mosia.nexus
package infrastructure.neuro

import domain.grpc.neuro.{HealthCheckRequest, NeuroOrchestratorServiceGrpc}
import domain.config.AppConfig
import domain.config.neuro.ClustersConfig
import domain.error.*
import domain.grpc.neuro.{HealthCheckRequest, NeuroOrchestratorServiceGrpc}
import domain.model.grpc.{ChannelPool, PoolHealth}

import java.util.concurrent.ConcurrentHashMap
import io.grpc.{ManagedChannel, ManagedChannelBuilder}
import zio.*

final class NeuroConnectionManager private (
  private val pools: Map[String, ChannelPool], // ← 不可变 Map
  config: AppConfig
):

  /** 获取指定 cluster 的 stub */
  def getStub(clusterId: String): AppTask[NeuroOrchestratorServiceGrpc.NeuroOrchestratorServiceStub] =
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

object NeuroConnectionManager:
  /** 创建 manager 的 ZLayer（scoped） */
  val live: ZLayer[AppConfig, AppError, NeuroConnectionManager] =
    ZLayer.scoped {
      for {
        config <- ZIO.service[AppConfig]

        // 预创建所有 cluster 的 pool
        poolList <- ZIO
          .foreachPar(config.neuro.clusters.toList) { case (clusterId, clusterCfg) =>
            ChannelPool
              .make(clusterCfg)
              .map(clusterId -> _)
              .tapError(e => ZIO.logError(s"Failed to create pool for cluster [$clusterId]: ${e.getMessage}"))
          }
          .mapError(toAppError)

        pools = poolList.toMap
        _ <- ZIO.logInfo(s"Initialized ${pools.size} neuro cluster pools")

        manager = NeuroConnectionManager(pools, config)

        // 使用全局配置的间隔
        _ <- healthCheckFiber(
          manager,
          config.neuro.healthCheckInterval
        ).forkScoped

      } yield manager
    }

  /** 单个 cluster 的健康检查 fiber */
  private def healthCheckFiber(
    manager: NeuroConnectionManager,
    interval: Duration
  ): ZIO[Any, Nothing, Unit] =
    val loop = for {
      // 检查所有 pool
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
      .catchAllCause(c =>
        ZIO.logErrorCause("Health check fiber failed", c) *>
          ZIO.unit
      )
