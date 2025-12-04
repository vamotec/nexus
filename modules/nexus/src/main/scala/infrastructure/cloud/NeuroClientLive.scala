package app.mosia.nexus
package infrastructure.cloud

import domain.config.AppConfig
import domain.error.*
import domain.grpc.neuro.*
import domain.grpc.neuro.NeuroOrchestratorServiceGrpc.NeuroOrchestratorServiceStub
import domain.model.grpc.ClusterTarget.Neuro
import domain.services.infra.{ClusterRegistry, NeuroClient}

import zio.{ZIO, ZLayer}

/** Neuro Orchestrator gRPC 客户端实现 使用 scalapb-zio-grpc 生成的客户端代码
  */
final class NeuroClientLive(
  manager: ConnectionManager,
  clusterRegistry: ClusterRegistry
) extends NeuroClient:

  private def withManualRouting[A](
    clusterId: String,
    f: NeuroOrchestratorServiceStub => scala.concurrent.Future[A]
  ): AppTask[A] =
    for {
      _ <- clusterRegistry.updateLoad(clusterId, +1)
      stub <- manager.getNeuroStub(clusterId)
      res <- ZIO
        .fromFuture(_ => f(stub))
        .mapError(toAppError)
        .ensuring(clusterRegistry.updateLoad(clusterId, -1))
    } yield res

  // 手动指定版本
  override def createSession(
    clusterId: String,
    request: CreateSessionRequest
  ): AppTask[CreateSessionResponse] =
    withManualRouting(clusterId, _.createSession(request))

  override def startSimulation(clusterId: String, request: StartSimulationRequest): AppTask[StartSimulationResponse] =
    withManualRouting(clusterId, _.startSimulation(request))

  override def stopSession(clusterId: String, request: StopSessionRequest): AppTask[StopSessionResponse] =
    withManualRouting(clusterId, _.stopSession(request))

  override def getSessionStatus(clusterId: String, request: GetSessionStatusRequest): AppTask[SessionStatusResponse] =
    withManualRouting(clusterId, _.getSessionStatus(request))

  override def createTrainingSession(
    clusterId: String,
    request: CreateTrainingSessionRequest
  ): AppTask[CreateTrainingSessionResponse] =
    withManualRouting(clusterId, _.createTrainingSession(request))

  override def createHybridSession(
    clusterId: String,
    request: CreateHybridSessionRequest
  ): AppTask[CreateHybridSessionResponse] =
    withManualRouting(clusterId, _.createHybridSession(request))

  override def getResourcePoolStatus(
    clusterId: String,
    request: ResourcePoolStatusRequest
  ): AppTask[ResourcePoolStatusResponse] =
    withManualRouting(clusterId, _.getResourcePoolStatus(request))

  override def healthCheck(clusterId: String, request: HealthCheckRequest): AppTask[HealthCheckResponse] =
    withManualRouting(clusterId, _.healthCheck(request))

object NeuroClientLive:
  /** 提供 NeuroClientLive 的 layer，按 clusterId 构建实例 */
  val live: ZLayer[ConnectionManager & ClusterRegistry, Nothing, NeuroClient] =
    ZLayer.scoped:
      for
        manager <- ZIO.service[ConnectionManager]
        registry <- ZIO.service[ClusterRegistry]
      yield NeuroClientLive(manager, registry)
      
  val layer: ZLayer[AppConfig & ClusterRegistry, AppError, NeuroClient] = ConnectionManager.make(Neuro) >>> live
