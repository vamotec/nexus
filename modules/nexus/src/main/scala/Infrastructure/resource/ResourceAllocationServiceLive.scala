package app.mosia.nexus
package infrastructure.resource

import domain.grpc.neuro.{
  CreateSessionRequest,
  CreateTrainingSessionRequest,
  StopSessionRequest,
  SessionMode as GrpcSessionMode
}
import domain.error.*
import domain.grpc.neuro.{
  CreateSessionRequest,
  CreateTrainingSessionRequest,
  StopSessionRequest,
  SessionMode as GrpcSessionMode
}
import domain.model.common.Priority
import domain.model.grpc.RoutingContext
import domain.model.resource.{ControlEndpoint, IsaacSimInstanceId, ResourceAssignment, StreamEndpoint}
import domain.model.session.{SessionId, SimSession}
import domain.model.training.{TrainingInstanceAssignment, TrainingInstanceId, TrainingJob}
import domain.model.user.UserId
import domain.services.infra.{GeoIpService, NeuroClient, ResourceAllocationService}

import zio.*

import java.net.URI
import java.time.Instant
import java.util.UUID
import io.grpc.Status
import scala.util.Try

/** 资源分配服务实现
  *
  * 负责与 Neuro 编排器交互，分配和释放资源
  *
  * 职责：
  *   - 为 Session 分配 Isaac Sim 实例
  *   - 为 TrainingJob 分配 Training 节点
  *   - 释放已分配的资源
  */
final class ResourceAllocationServiceLive(
  neuroClient: NeuroClient
) extends ResourceAllocationService:

  /** 为会话分配 Isaac Sim 实例
    *
    * 调用 Neuro 编排器的 createSession API
    *
    * @param session
    *   仿真会话
    * @return
    *   ResourceAssignment 包含 Isaac Sim 实例 ID 和端点信息
    */
  override def allocateIsaacSimInstance(session: SimSession, clusterId: String): AppTask[ResourceAssignment] =
    for
      // 1. 准备创建会话请求
      // TODO: 将 SimulationConfigSnapshot 转换为 common.GrpcSceneConfig
      request <- ZIO.succeed(
        CreateSessionRequest(
          sessionId = session.id.value.toString,
          mode = GrpcSessionMode.MANUAL,
          sceneConfig = None, // TODO: 从 session.configSnapshot 提取场景配置
          streamConfig = None // TODO: 添加流配置
        )
      )
      // 2. 调用 Neuro 创建会话
      response <- neuroClient.createSession(clusterId, request)

      // 3. 检查是否成功
      _ <- ZIO
        .fail(GrpcServiceError("neuro", "create", Status.NOT_FOUND))
        .when(!response.success)

      // 4. 解析 instanceId 字符串为 UUID
      instanceUuid <- ZIO
        .fromTry(Try(UUID.fromString(response.instanceId)))
        .mapError(err =>
          InvalidInput(
            field = "uuid",
            reason = s"Invalid instance ID: ${response.instanceId}",
            cause = Some(err)
          )
        )

      // 5. 构建 StreamEndpoint（从 proto.StreamEndpoint 提取 host 和 port）
      streamEndpoint <- response.streamEndpoint match
        case Some(protoStream) =>
          // proto.StreamEndpoint 有 url 字段，需要解析出 host
          val host = extractHostFromUrl(protoStream.url).getOrElse("localhost")
          ZIO.succeed(
            StreamEndpoint(
              host = host,
              port = protoStream.port,
              protocol = protoStream.protocol
            )
          )
        case None =>
          // 如果没有 streamEndpoint，使用 webrtcSignalingUrl 推断
          val host = extractHostFromUrl(response.webrtcSignalingUrl).getOrElse("localhost")
          ZIO.succeed(
            StreamEndpoint(
              host = host,
              port = 8080, // 默认端口
              protocol = "webrtc"
            )
          )

      // 6. 构建资源分配对象
      assignment = ResourceAssignment(
        isaacSimInstanceId = IsaacSimInstanceId(instanceUuid),
        nucleusPath = response.nucleusPath, // 从 Neuro 返回的 Nucleus 路径
        streamEndpoint = streamEndpoint,
        controlEndpoint = ControlEndpoint(wsUrl = response.controlWsUrl)
      )

      // 7. 记录日志
      _ <- ZIO.logInfo(s"Allocated Isaac Sim instance ${response.instanceId} for session ${session.id.value}")
    yield assignment

  /** 为训练任务分配训练实例
    *
    * 调用 Neuro 编排器的 createTrainingSession API
    *
    * @param job
    *   训练任务
    * @return
    *   TrainingInstanceAssignment 包含实例 ID 和 GPU 信息
    */
  override def allocateTrainingInstance(job: TrainingJob, clusterId: String): AppTask[TrainingInstanceAssignment] =
    for
      // 1. 准备创建训练会话请求
      // TODO: 需要从 TrainingJob 提取 GrpcSceneConfig、GrpcTrainingConfig、GrpcResourceRequirements
      request <- ZIO.succeed(
        CreateTrainingSessionRequest(
          sessionId = job.sessionId.value.toString,
          mode = GrpcSessionMode.TRAINING,
          sceneConfig = None, // TODO: 添加场景配置
          trainingConfig = None, // TODO: 从 job.config 和 job.algorithm 转换
          resourceRequirements = None // TODO: 添加资源需求
        )
      )

      // 2. 调用 Neuro 创建训练会话
      response <- neuroClient.createTrainingSession(clusterId, request)

      // 3. 检查是否成功
      _ <- ZIO
        .fail(GrpcServiceError("neuro", "create", Status.NOT_FOUND))
        .when(!response.success)

      // 4. 解析 trainingInstanceId 字符串为 UUID
      instanceUuid <- ZIO
        .fromTry(Try(UUID.fromString(response.trainingInstanceId)))
        .mapError(err =>
          InvalidInput("uuid", s"Invalid training instance ID format: ${response.trainingInstanceId} $err")
        )

      // 5. 构建训练实例分配对象
      assignment = TrainingInstanceAssignment(
        instanceId = Some(TrainingInstanceId(instanceUuid)),
        gpuIds = response.allocatedGpuIds.toList, // Seq[Int] → List[Int]
        assignedAt = Instant.now()
      )

      // 6. 记录日志
      _ <- ZIO.logInfo(
        s"Allocated training instance ${response.trainingInstanceId} " +
          s"with GPUs [${response.allocatedGpuIds.mkString(", ")}] " +
          s"for job ${job.id.value}"
      )
    yield assignment

  /** 释放会话资源 */
  override def releaseResources(session: SimSession): AppTask[Unit] =
    for
      // 1. 准备停止会话请求
      request <- ZIO.succeed(
        StopSessionRequest(
          sessionId = session.id.value.toString,
          cleanup = true
        )
      )
      // 2. 调用 Neuro 停止会话（释放资源）
      response <- neuroClient.stopSession(session.clusterId, request)

      // 3. 检查是否成功（失败时仅警告，不抛异常）
      _ <- ZIO
        .logWarning(s"Failed to release resources: ${response.message}")
        .when(!response.success)

      // 4. 记录日志
      _ <- ZIO.logInfo(s"Released resources for session ${session.id.value}")
    yield ()

  override def startSimulation(session: SimSession): AppTask[Unit] =
    for
      request <- ZIO.succeed(
        domain.grpc.neuro.StartSimulationRequest(
          sessionId = session.id.value.toString
        )
      )
      response <- neuroClient.startSimulation(session.clusterId, request)
      // 3. 检查是否成功（失败时仅警告，不抛异常）
      _ <- ZIO
        .logWarning(s"Failed to start resources: ${response.message}")
        .when(!response.success)

      // 4. 记录日志
      _ <- ZIO.logInfo(s"Start resources for session ${session.id.value}")
    yield ()

  /** 辅助方法：从 URL 字符串提取 host
    *
    * 支持解析形如 "ws://isaac-sim-1:8766/control" 的 URL
    */
  private def extractHostFromUrl(url: String): Option[String] =
    Try {
      val uri = new URI(url)
      Option(uri.getHost).orElse {
        // 如果 URI 解析失败，尝试简单的字符串匹配
        url
          .split("://")
          .lastOption
          .flatMap(_.split("/").headOption)
          .flatMap(_.split(":").headOption)
      }
    }.toOption.flatten

object ResourceAllocationServiceLive:
  val live: ZLayer[NeuroClient, Nothing, ResourceAllocationServiceLive] =
    ZLayer.fromFunction(ResourceAllocationServiceLive(_))
