package app.mosia.nexus.application.service.webRTC

import app.mosia.nexus.domain.model.session.SignalingMessage
import zio.ZIO
import zio.http.{ChannelEvent, Handler, WebSocketApp, WebSocketFrame}
import zio.json.*

object WebRTCSignalingServer:
  // WebSocket 消息处理
  def websocketHandler(sessionId: String): WebSocketApp[SignalingService] =
    Handler.webSocket: channel =>
      for
        service <- ZIO.service[SignalingService]
        _ <- service.registerClient(sessionId, channel)
        // 处理客户端消息
        _ <- channel.receiveAll:
          case ChannelEvent.Read(WebSocketFrame.Text(text)) =>
            parseMessage(text) match
              case Some(message) =>
                service.broadcastMessage(message)
              case None =>
                ZIO.logError(s"Failed to parse message: $text")
          case ChannelEvent.Unregistered =>
            service.unregisterClient(sessionId) *>
              ZIO.logInfo(s"Client disconnected: $sessionId")
          case _ => ZIO.unit
      yield ()

  // JSON 消息解析
  private def parseMessage(text: String): Option[SignalingMessage] =
    // 先解析为通用 JSON 对象
    text
      .fromJson[Map[String, String]]
      .toOption
      .flatMap: json =>
        json.get("type") match
          case Some("offer") =>
            text.fromJson[SignalingMessage.Offer].toOption
          case Some("answer") =>
            text.fromJson[SignalingMessage.Answer].toOption
          case Some("ice") =>
            text.fromJson[SignalingMessage.IceCandidate].toOption
          case Some("start") =>
            text.fromJson[SignalingMessage.StartSimulation].toOption
          case Some("stop") =>
            text.fromJson[SignalingMessage.StopSimulation].toOption
          case _ => None
