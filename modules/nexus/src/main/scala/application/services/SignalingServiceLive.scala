package app.mosia.nexus
package application.services

import application.states.SessionState
import domain.model.resource.ClientType
import domain.model.session.SignalingMessage
import domain.model.session.SignalingMessage.*
import domain.services.app.SignalingService

import zio.json.*
import zio.*
import zio.http.*

final class SignalingServiceLive(sessions: Ref[Map[String, SessionState]]) extends SignalingService:

  override def registerClient(sessionId: String, clientType: ClientType, channel: WebSocketChannel): UIO[Unit] =
    sessions.update: sessionMap =>
      val currentState = sessionMap.getOrElse(sessionId, SessionState(None, None))
      val updatedState = clientType match
        case ClientType.Frontend => currentState.copy(frontend = Some(channel))
        case ClientType.IsaacSim => currentState.copy(isaacSim = Some(channel))
      sessionMap + (sessionId -> updatedState)
    *> ZIO.logInfo(s"Client registered: sessionId=$sessionId, type=$clientType")

  override def unregisterClient(sessionId: String, clientType: ClientType): UIO[Unit] =
    sessions.update: sessionMap =>
      sessionMap.get(sessionId) match
        case Some(state) =>
          val updatedState = clientType match
            case ClientType.Frontend => state.copy(frontend = None)
            case ClientType.IsaacSim => state.copy(isaacSim = None)
          // 如果两个客户端都已断开,则删除会话
          if updatedState.frontend.isEmpty && updatedState.isaacSim.isEmpty then sessionMap - sessionId
          else sessionMap + (sessionId -> updatedState)
        case None =>
          sessionMap
    *> ZIO.logInfo(s"Client unregistered: sessionId=$sessionId, type=$clientType")

  override def sendToFrontend(sessionId: String, message: SignalingMessage): UIO[Unit] =
    sessions.get.flatMap: sessionMap =>
      sessionMap.get(sessionId).flatMap(_.frontend) match
        case Some(channel) =>
          sendMessage(channel, message)
        case None =>
          ZIO.logWarning(s"Frontend not connected for session: $sessionId")

  override def sendToIsaacSim(sessionId: String, message: SignalingMessage): UIO[Unit] =
    sessions.get.flatMap: sessionMap =>
      sessionMap.get(sessionId).flatMap(_.isaacSim) match
        case Some(channel) =>
          sendMessage(channel, message)
        case None =>
          ZIO.logWarning(s"Isaac Sim not connected for session: $sessionId")

  override def relayMessage(sessionId: String, fromClientType: ClientType, message: SignalingMessage): UIO[Unit] =
    fromClientType match
      case ClientType.Frontend =>
        // Frontend 发送的消息转发给 Isaac Sim
        sendToIsaacSim(sessionId, message) *>
          ZIO.logDebug(
            s"Relayed message from Frontend to Isaac Sim: $sessionId, type=${message.getClass.getSimpleName}"
          )
      case ClientType.IsaacSim =>
        // Isaac Sim 发送的消息转发给 Frontend
        sendToFrontend(sessionId, message) *>
          ZIO.logDebug(
            s"Relayed message from Isaac Sim to Frontend: $sessionId, type=${message.getClass.getSimpleName}"
          )

  override def isSessionReady(sessionId: String): UIO[Boolean] =
    sessions.get.map: sessionMap =>
      sessionMap.get(sessionId).exists(_.isReady)

  // 私有辅助方法: 发送消息到 WebSocket 通道
  private def sendMessage(channel: WebSocketChannel, message: SignalingMessage): UIO[Unit] =
    val json = messageToJson(message)
    channel.send(ChannelEvent.Read(WebSocketFrame.text(json))).ignore

  // 私有辅助方法: 将消息转换为 JSON
  private def messageToJson(message: SignalingMessage): String =
    message match
      case o: Offer => o.toJson
      case a: Answer => a.toJson
      case i: IceCandidate => i.toJson
      case s: StartSimulation => s.toJson
      case st: StopSimulation => st.toJson
      case r: Ready => r.toJson
      case e: Error => e.toJson

object SignalingServiceLive:
  val live: ZLayer[Any, Nothing, SignalingService] =
    ZLayer:
      for sessions <- Ref.make(Map.empty[String, SessionState])
      yield SignalingServiceLive(sessions)
