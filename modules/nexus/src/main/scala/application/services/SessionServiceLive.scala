package app.mosia.nexus
package application.services

import application.dto.request.session.CreateSessionRequest
import application.dto.response.scene.SceneResponse
import application.dto.response.session.*
import domain.error.*
import domain.model.grpc.RoutingContext
import domain.model.metrics.SimSessionMetrics
import domain.model.project.ProjectId
import domain.model.scene.SceneConfig
import domain.model.session.*
import domain.model.simulation.SimulationId
import domain.model.user.UserId
import domain.repository.{ProjectRepository, SessionMetricsRepository, SessionRepository, SimulationRepository}
import domain.services.app.SessionService
import domain.services.infra.*

import zio.json.*
import zio.*

import java.time.Instant

/** 会话管理应用服务
  *
  * 职责：协调 Domain、Repository 和外部服务
  *
  * 依赖说明：
  *   - ResourceAllocationService: 资源分配和释放（创建/停止会话时调用）
  *   - NeuroClient: 状态控制和查询（启动仿真、获取状态等）
  */
final class SessionServiceLive(
  sessionRepository: SessionRepository,
  simulationRepository: SimulationRepository,
  projectRepository: ProjectRepository,
  sessionMetricsRepository: SessionMetricsRepository,
  resourceAllocation: ResourceAllocationService,
  jwtService: JwtService,
  geoIpService: GeoIpService,
  routingStrategy: ClusterRoutingStrategy
) extends SessionService:

  private def getContext(userId: UserId, projectId: ProjectId, userIp: String): AppTask[RoutingContext] =
    ZIO.scoped:
      for
        // 1. 从 IP 获取地理位置（可以用 GeoIP 库）
        userLocation <- geoIpService.lookup(userIp)
        projectOpt <- projectRepository.findById(projectId)
        project <- ZIO.fromOption(projectOpt).mapError(_ => NotFound("Project", projectId.value.toString))
        // 2. 构建路由上下文
        context = RoutingContext(
          userId = userId,
          projectId = projectId,
          project = project,
          userLocation = userLocation
        )
      yield context

  /** 创建会话（完整流程） */
  override def createSession(
    userId: UserId,
    simulationId: SimulationId,
    request: CreateSessionRequest,
    userIp: String
  ): AppTask[SessionResponse] =
    (for
      // 1. 生成会话 ID
      sessionId <- ZIO.succeed(SessionId.generate())
      projectId <- ProjectId.fromString(request.projectId)
      context <- getContext(userId, projectId, userIp)
      clusterId <- routingStrategy.selectCluster(context)
      // 2. 解析会话模式
      mode <- ZIO
        .attempt(SessionMode.fromString(request.mode))
        .mapError(e => new IllegalArgumentException(s"Invalid session mode: ${request.mode}", e))

      // 3. 获取 Simulation 配置（创建快照）
      simulation <- simulationRepository
        .findById(simulationId)
        .someOrFail(new RuntimeException(s"Simulation not found: ${simulationId.value}"))

      // 4. 创建配置快照
      configSnapshot = SimulationConfigSnapshot(
        simulationId = simulation.id.value,
        simulationName = simulation.name,
        version = s"${simulation.version.major}.${simulation.version.minor}",
        sceneConfig = simulation.sceneConfig,
        simulationParams = simulation.simulationParams,
        snapshotAt = Instant.now()
      )

      // 5. 创建领域对象 SimSession（初始状态：Pending）
      now     = Instant.now()
      session = SimSession(
        id = sessionId,
        simulationId = simulationId,
        projectId = projectId,
        userId = userId,
        clusterId = clusterId,
        mode = mode,
        configSnapshot = configSnapshot,
        status = SessionStatus.Pending,
        error = None,
        resourceAssignment = None, // 等待 Neuro 分配
        result = None,
        createdAt = now,
        startedAt = None,
        completedAt = None
      )

      // 6. 保存到数据库
      _ <- sessionRepository.save(session)

      // 7. 使用 ResourceAllocationService 分配 Isaac Sim 实例
      resourceAssignment <- resourceAllocation.allocateIsaacSimInstance(session, clusterId)

      // 8. 更新会话状态（添加资源分配信息）
      updatedSession = session.copy(
        resourceAssignment = Some(resourceAssignment),
        status = SessionStatus.Initializing
      )
      _ <- sessionRepository.update(updatedSession)

      // 9. 根据模式生成 control token 和 endpoints (仅 Manual/Hybrid 需要 WebRTC)
      controlToken <-
        if mode.needsWebRTC then
          jwtService.generateSessionToken(
            sessionId = sessionId,
            userId = userId,
            permissions = Set("control", "view", "metrics")
          )
        else ZIO.succeed("")

      // 10. 构建响应
      response = SessionResponse(
        id = sessionId.value.toString,
        userId = userId.value.toString,
        projectId = request.projectId,
        mode = mode.toString.toLowerCase,
        status = SessionStatus.Initializing,
        scene = domainToSceneResponse(simulation.sceneConfig),
        streamEndpoint =
          if mode.needsWebRTC then
            Some(
              StreamEndpointResponse(
                protocol = resourceAssignment.streamEndpoint.protocol,
                url = resourceAssignment.streamEndpoint.host,
                port = resourceAssignment.streamEndpoint.port,
                host = resourceAssignment.streamEndpoint.host
              )
            )
          else None,
        controlEndpoint =
          if mode.needsWebRTC then
            Some(
              ControlEndpointResponse(
                controlWsUrl = resourceAssignment.controlEndpoint.wsUrl,
                controlToken = controlToken,
                webrtcSignalingUrl = s"ws://nexus:8080/api/webrtc/signaling/${sessionId.value}"
              )
            )
          else None,
        createdAt = updatedSession.createdAt.toEpochMilli,
        startedAt = None
      )
    yield response).mapError(toAppError)

  /** 启动会话 */
  override def startSession(sessionId: SessionId): AppTask[SessionResponse] =
    (for
      // 1. 获取会话
      sessionOpt <- sessionRepository.findById(sessionId)
      session <- ZIO
        .fromOption(sessionOpt)
        .orElseFail(new NoSuchElementException(s"Session not found: ${sessionId.value}"))

      // 2. 验证状态
      _ <- ZIO.when(session.status != SessionStatus.Initializing)(
        ZIO.fail(new IllegalStateException(s"Cannot start session in status: ${session.status}"))
      )

      _ <- resourceAllocation.startSimulation(session)

      // 4. 更新会话状态
      updatedSession = session.copy(
        status = SessionStatus.Running,
        startedAt = Some(Instant.now())
      )
      _ <- sessionRepository.update(updatedSession)

      // 5. 生成响应
      response <- buildSessionResponse(updatedSession)
    yield response).mapError(toAppError)

  /** 停止会话 */
  override def stopSession(sessionId: SessionId, reason: String = "User requested"): AppTask[SessionResponse] =
    (for
      // 1. 获取会话
      sessionOpt <- sessionRepository.findById(sessionId)
      session <- ZIO
        .fromOption(sessionOpt)
        .orElseFail(new NoSuchElementException(s"Session not found: ${sessionId.value}"))

      // 2. 验证状态（只能停止运行中或暂停的会话）
      _ <- ZIO.when(!session.status.isActive && session.status != SessionStatus.Initializing)(
        ZIO.fail(new IllegalStateException(s"Cannot stop session in status: ${session.status}"))
      )

      // 3. 使用 ResourceAllocationService 释放资源
      _ <- resourceAllocation.releaseResources(session)

      // 4. 更新会话状态
      updatedSession = session.copy(
        status = SessionStatus.Stopped,
        completedAt = Some(Instant.now())
      )
      _ <- sessionRepository.update(updatedSession)

      // 5. 生成响应
      response <- buildSessionResponse(updatedSession)
    yield response).mapError(toAppError)

  /** 获取会话详情 */
  override def getSessionById(sessionId: SessionId): AppTask[Option[SessionResponse]] =
    (for
      sessionOpt <- sessionRepository.findById(sessionId)
      responseOpt <- sessionOpt match
        case Some(session) => buildSessionResponse(session).map(Some(_))
        case None => ZIO.succeed(None)
    yield responseOpt).mapError(toAppError)

  /** 列出用户的所有会话 */
  override def listUserSessions(
    userId: UserId,
    projectId: ProjectId,
    limit: Int = 100
  ): AppTask[List[SessionResponse]] =
    (for
      // 查询用户的所有会话（按创建时间倒序）
      sessions <- sessionRepository.findByUserId(userId, 20)

      // 过滤项目
      filtered = sessions.filter(_.projectId == projectId)

      // 限制数量并转换为响应
      limited = filtered.sortBy(_.createdAt.toEpochMilli)(using Ordering[Long].reverse).take(limit)
      responses <- ZIO.foreach(limited)(buildSessionResponse)
    yield responses).mapError(toAppError)

  // ============================================================================
  // 辅助方法 - Domain ↔ DTO 转换
  // ============================================================================

  /** 构建 SessionResponse */
  private def buildSessionResponse(session: SimSession): Task[SessionResponse] =
    for
      // 根据模式生成 control token (仅 Manual/Hybrid 需要)
      controlToken <-
        if session.mode.needsWebRTC then
          jwtService.generateSessionToken(
            sessionId = session.id,
            userId = session.userId,
            permissions = Set("control", "view", "metrics")
          )
        else ZIO.succeed("")
    yield SessionResponse(
      id = session.id.value.toString,
      userId = session.userId.value.toString,
      projectId = session.projectId.value.toString,
      mode = session.mode.toString.toLowerCase,
      status = session.status,
      scene = domainToSceneResponse(session.configSnapshot.sceneConfig),
      streamEndpoint =
        if session.mode.needsWebRTC then
          session.resourceAssignment.map(ra =>
            StreamEndpointResponse(
              protocol = ra.streamEndpoint.protocol,
              url = ra.streamEndpoint.host,
              port = ra.streamEndpoint.port,
              host = ra.streamEndpoint.host
            )
          )
        else None,
      controlEndpoint =
        if session.mode.needsWebRTC then
          session.resourceAssignment.map(ra =>
            ControlEndpointResponse(
              controlWsUrl = ra.controlEndpoint.wsUrl,
              controlToken = controlToken,
              webrtcSignalingUrl = s"ws://nexus:8080/api/webrtc/signaling/${session.id.value}"
            )
          )
        else None,
      createdAt = session.createdAt.toEpochMilli,
      startedAt = session.startedAt.map(_.toEpochMilli)
    )

  /** Domain SessionMetrics → MetricsResponse */
  private def domainMetricsToResponse(metrics: SimSessionMetrics): SessionMetricsResponse =
    SessionMetricsResponse(
      fps = metrics.currentFps,
      frameCount = metrics.frameCount,
      simulationTime = metrics.simulationTime,
      wallTime = metrics.wallTime,
      timeRatio = metrics.timeRatio,
      gpuUtilization = metrics.gpuUtilization,
      gpuMemoryMB = metrics.gpuMemoryMB,
      robotPosition = Some(metrics.robotPosition),
      lastUpdatedAt = metrics.updatedAt.toEpochMilli
    )

  /** Domain SceneConfig → SceneResponse */
  private def domainToSceneResponse(sceneConfig: SceneConfig): SceneResponse =
    SceneResponse(
      name = sceneConfig.name,
      robotType = sceneConfig.robotType.toString,
      environment = sceneConfig.environment.environmentType.toString,
      obstacleCount = sceneConfig.obstacles.length,
      sensorCount = sceneConfig.sensors.length
    )

object SessionServiceLive:
  val live: ZLayer[
    SessionRepository & SimulationRepository & SessionMetricsRepository & ProjectRepository &
      ResourceAllocationService & NeuroClient & JwtService & GeoIpService & ClusterRoutingStrategy,
    Nothing,
    SessionServiceLive
  ] =
    ZLayer.fromFunction(SessionServiceLive(_, _, _, _, _, _, _, _))
