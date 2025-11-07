package app.mosia.nexus.application.service.webRTC

import app.mosia.nexus.domain.model.session.SignalingMessage
import app.mosia.nexus.domain.model.session.SignalingMessage.*
import zio.{Ref, UIO, ZIO, ZLayer}
import zio.http.{ChannelEvent, WebSocketChannel, WebSocketFrame}
import zio.json.EncoderOps

final class SignalingServiceLive(clients: Ref[Map[String, WebSocketChannel]]) extends SignalingService:
  override def registerClient(sessionId: String, channel: WebSocketChannel): UIO[Unit] =
    clients.update(_ + (sessionId -> channel)) *>
      ZIO.logInfo(s"Client registered: $sessionId")

  override def unregisterClient(sessionId: String): UIO[Unit] =
    clients.update(_ - sessionId) *>
      ZIO.logInfo(s"Client unregistered: $sessionId")

  override def sendToClient(sessionId: String, message: SignalingMessage): UIO[Unit] =
    clients.get.flatMap: clientMap =>
      clientMap.get(sessionId) match
        case Some(channel) =>
          val json = message match
            case o: Offer => o.toJson
            case a: Answer => a.toJson
            case i: IceCandidate => i.toJson
            case s: StartSimulation => s.toJson
            case st: StopSimulation => st.toJson
          channel.send(ChannelEvent.Read(WebSocketFrame.text(json))).ignore
        case None =>
          ZIO.logWarning(s"Client not found: $sessionId")

  override def broadcastMessage(message: SignalingMessage): UIO[Unit] =
    message match
      case msg @ Offer(sessionId, _) => sendToClient(sessionId, msg)
      case msg @ Answer(sessionId, _) => sendToClient(sessionId, msg)
      case msg @ IceCandidate(sessionId, _, _, _) => sendToClient(sessionId, msg)
      case msg @ StartSimulation(sessionId, _) =>
        ZIO.logInfo(s"Starting simulation for $sessionId") *>
          sendToClient(sessionId, msg)
      case msg @ StopSimulation(sessionId) =>
        ZIO.logInfo(s"Stopping simulation for $sessionId") *>
          sendToClient(sessionId, msg)

object SignalingServiceLive:
  val live: ZLayer[Any, Nothing, SignalingService] =
    ZLayer:
      for clients <- Ref.make(Map.empty[String, WebSocketChannel])
      yield SignalingServiceLive(clients)
