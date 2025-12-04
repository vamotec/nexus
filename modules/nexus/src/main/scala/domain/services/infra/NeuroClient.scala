package app.mosia.nexus
package domain.services.infra

import domain.error.AppTask
import domain.grpc.neuro.*

/** Neuro Orchestrator gRPC 客户端 负责与 Neuro 编排器通信，管理 Isaac Sim 和 Training Instance 资源池
  */
trait NeuroClient:
  // ============================================================================
  // Manual 模式 - 手动控制 自动选择不在这一层实现，在创建session时就绑定
  // ============================================================================

  def createSession(
    clusterId: String,
    request: CreateSessionRequest
  ): AppTask[CreateSessionResponse]

  def startSimulation(
    clusterId: String,
    request: StartSimulationRequest
  ): AppTask[StartSimulationResponse]

  def stopSession(
    clusterId: String,
    request: StopSessionRequest
  ): AppTask[StopSessionResponse]

  def getSessionStatus(
    clusterId: String,
    request: GetSessionStatusRequest
  ): AppTask[SessionStatusResponse]

  def createTrainingSession(
    clusterId: String,
    request: CreateTrainingSessionRequest
  ): AppTask[CreateTrainingSessionResponse]

  def createHybridSession(
    clusterId: String,
    request: CreateHybridSessionRequest
  ): AppTask[CreateHybridSessionResponse]

  def getResourcePoolStatus(
    clusterId: String,
    request: ResourcePoolStatusRequest
  ): AppTask[ResourcePoolStatusResponse]

  def healthCheck(
    clusterId: String,
    request: HealthCheckRequest
  ): AppTask[HealthCheckResponse]
