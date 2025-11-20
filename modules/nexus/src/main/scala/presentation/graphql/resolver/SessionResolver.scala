package app.mosia.nexus
package presentation.graphql.resolver

import application.dto.response.session.{RobotPositionUpdate, SessionMetricsResponse}
import domain.error.AppError
import domain.model.common.{Quaternion, Vector3D}
import domain.model.session.{SessionId, SessionStatus}
import domain.model.simulation.SimulationId
import domain.model.user.UserId
import domain.services.app.SessionService
import presentation.graphql.schema.SessionSchema.*

import caliban.CalibanError
import zio.{Duration, Schedule, ZIO, durationInt}
import zio.stream.ZStream

import java.time.Instant

/** Session GraphQL Resolver
  *
  * 负责将 GraphQL 查询/变更映射到 SessionService
  */
object SessionResolver:

  def queries(service: SessionService) = SessionQueries(
    // 根据 ID 查询单个会话
    session = sessionId =>
      (for
        id <- SessionId.fromString(sessionId)
        response <- service.getSessionById(id)
      yield response).mapError(_.toCalibanError),
    // 查询指定仿真的所有会话（最近 N 个）
    sessionsBySimulation = args =>
      // TODO: 需要在 SessionService 添加 findBySimulationId 方法
      // 暂时返回空列表
      ZIO.succeed(List.empty),

    // 查询当前用户的会话（根据状态过滤）
    mySessions = args =>
      // TODO: 需要从 GraphQL Context 获取当前用户 ID
      // 暂时返回空列表
      ZIO.succeed(List.empty)
  )

  def mutations(service: SessionService) = SessionMutations(
    // 创建新会话
    create = args =>
      (for
        uId <- UserId.fromString(args.userId)
        sId <- SimulationId.fromString(args.simulationId)
        res <- service.createSession(
          userId = uId,
          simulationId = sId,
          request = args.request,
          userIp = args.userIp
        )
      yield res).mapError(_.toCalibanError),
    // 启动会话
    start = sessionId =>
      (for
        id <- SessionId.fromString(sessionId)
        response <- service.startSession(id)
      yield response).mapError(_.toCalibanError),
    // 暂停会话
    pause = sessionId =>
      // TODO: 需要在 SessionService 添加 pauseSession 方法
      // 暂时使用 stop 作为 workaround
      (for
        id <- SessionId.fromString(sessionId)
        response <- service.stopSession(id, reason = "Paused by user")
      yield response).mapError(_.toCalibanError),
    // 恢复会话
    resume = sessionId =>
      // TODO: 需要在 SessionService 添加 resumeSession 方法
      // 暂时使用 start 作为 workaround
      (for
        id <- SessionId.fromString(sessionId)
        response <- service.startSession(id)
      yield response).mapError(_.toCalibanError),
    // 停止会话
    stop = sessionId =>
      (for
        id <- SessionId.fromString(sessionId)
        response <- service.stopSession(id)
      yield response).mapError(_.toCalibanError)
  )

  def subscriptions(service: SessionService) = SessionSubscriptions(
    // 会话状态更新订阅
    sessionUpdates = sessionIdStr =>
      ZStream.unwrap {
        SessionId
          .fromString(sessionIdStr)
          .mapError(e => CalibanError.ExecutionError(e.getMessage))
          .map { sessionId =>
            ZStream
              .repeatZIOWithSchedule(
                service
                  .getSessionById(sessionId)
                  .someOrFail(CalibanError.ExecutionError(s"Session not found: $sessionId")),
                Schedule.spaced(2.second)
              )
              .mapError {
                case e: AppError => e.toCalibanError
                case e: Throwable => CalibanError.ExecutionError(e.getMessage)
              }
          }
      },

    // 会话指标流订阅
    sessionMetricsStream = args =>
      val interval = Duration.fromMillis(args.intervalMs.getOrElse(1000).toLong)

      // 定期获取指标（实际应该从 TimescaleDB 或实时数据流读取）
      ZStream
        .repeatZIOWithSchedule(
          ZIO.succeed(generateMockMetrics(args.sessionId)),
          zio.Schedule.spaced(interval)
        )
    ,

    // 机器人位置订阅
    robotPosition = args =>
      val interval = Duration.fromMillis(args.intervalMs.getOrElse(100).toLong)

      // 高频位置更新流（实际应该从仿真引擎的实时数据流读取）
      ZStream
        .repeatZIOWithSchedule(
          ZIO.succeed(generateMockRobotPosition(args.sessionId, args.robotId)),
          zio.Schedule.spaced(interval)
        )
  )

  // ============ 辅助方法 ============
  // 注意：以下是 Mock 实现，实际应该从真实数据源读取

  private def generateMockMetrics(sessionId: String): SessionMetricsResponse =
    SessionMetricsResponse(
      fps = 60.0 + scala.util.Random.nextDouble() * 10,
      frameCount = System.currentTimeMillis() / 16,
      simulationTime = System.currentTimeMillis() / 1000.0,
      wallTime = System.currentTimeMillis() / 1000.0,
      timeRatio = 1.0,
      gpuUtilization = 70.0 + scala.util.Random.nextDouble() * 20,
      gpuMemoryMB = 4000 + scala.util.Random.nextInt(1000),
      robotPosition = Some(
        domain.model.common.Position3D(
          x = scala.util.Random.nextDouble() * 10,
          y = 0.0,
          z = scala.util.Random.nextDouble() * 10
        )
      ),
      lastUpdatedAt = System.currentTimeMillis()
    )

  private def generateMockRobotPosition(sessionId: String, robotId: Option[String]): RobotPositionUpdate =
    val time = System.currentTimeMillis()
    val t    = (time / 1000.0) % (2 * Math.PI)

    RobotPositionUpdate(
      sessionId = sessionId,
      robotId = robotId.getOrElse("robot-0"),
      position = Vector3D(
        x = Math.cos(t) * 5,
        y = 0.0,
        z = Math.sin(t) * 5
      ),
      orientation = Quaternion(0, 0, Math.sin(t / 2), Math.cos(t / 2)),
      linearVelocity = Vector3D(
        x = -Math.sin(t) * 0.5,
        y = 0.0,
        z = Math.cos(t) * 0.5
      ),
      angularVelocity = Vector3D(0, 0.1, 0),
      timestamp = time,
      simulationTime = time / 1000.0
    )
