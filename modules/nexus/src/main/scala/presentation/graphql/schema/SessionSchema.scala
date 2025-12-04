package app.mosia.nexus
package presentation.graphql.schema

import application.dto.request.session.CreateSessionRequest
import application.dto.response.session.{
  RobotPositionUpdate,
  SessionMetricsResponse,
  SessionResponse,
  SessionResultResponse
}
import domain.error.CalTask
import domain.model.metrics.SimSessionMetrics
import domain.model.session.{SessionId, SessionStatus}
import domain.model.simulation.{Simulation, SimulationId}
import domain.model.training.TrainingProgress
import domain.model.user.UserId

import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json
import zio.stream.ZStream

object SessionSchema:
  case class sessionsBySimulationArgs(
    id: String,
    count: Int
  ) derives Cs.SemiAuto,
        ArgBuilder

  case class MySessionsArgs(
    count: Int,
    status: SessionStatus
  ) derives Cs.SemiAuto,
        ArgBuilder

  case class MetricsArgs(
    sessionId: String,
    timeRange: String
  ) derives Cs.SemiAuto,
        ArgBuilder

  case class SessionQueries(
    session: String => CalTask[Option[SessionResponse]],
    sessionsBySimulation: sessionsBySimulationArgs => CalTask[List[SessionResponse]],
    mySessions: MySessionsArgs => CalTask[List[SessionResponse]]
//    sessionMetrics: MetricsArgs => CalTask[SessionMetricsResponse],
//    sessionResult: SessionId => CalTask[SessionResultResponse],
//    myActiveSession: SessionId => CalTask[Option[SessionResponse]]
  ) derives Cs.SemiAuto

  case class CreateSessionArgs(
    userId: String,
    simulationId: String,
    request: CreateSessionRequest,
    userIp: String
  ) derives Cs.SemiAuto,
        ArgBuilder

  case class SessionMutations(
    create: CreateSessionArgs => CalTask[SessionResponse],
    start: String => CalTask[SessionResponse],
    pause: String => CalTask[SessionResponse],
    resume: String => CalTask[SessionResponse],
    stop: String => CalTask[SessionResponse]
  ) derives Cs.SemiAuto

  case class SessionMetricsStreamArgs(
    sessionId: String,
    intervalMs: Option[Int] = Some(1000) // 默认 1 秒推送一次
  ) derives Cs.SemiAuto,
        ArgBuilder

  case class RobotPositionArgs(
    sessionId: String,
    robotId: Option[String] = None, // 如果不指定，返回所有机器人
    intervalMs: Option[Int] = Some(100) // 默认 100ms 推送一次
  ) derives Cs.SemiAuto,
        ArgBuilder

  /** GraphQL 订阅 */
  case class SessionSubscriptions(
    // 会话状态更新订阅
    sessionUpdates: String => ZStream[Any, Throwable, SessionResponse],

    // 会话指标流订阅（实时性能数据）
    sessionMetricsStream: SessionMetricsStreamArgs => ZStream[Any, Throwable, SessionMetricsResponse],

    // 机器人位置订阅（高频更新）
    robotPosition: RobotPositionArgs => ZStream[Any, Throwable, RobotPositionUpdate]
  ) derives Cs.SemiAuto
