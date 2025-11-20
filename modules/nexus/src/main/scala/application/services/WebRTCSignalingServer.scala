package app.mosia.nexus
package application.services

import domain.model.resource.ClientType
import domain.model.session.SignalingMessage
import domain.services.app.SignalingService
import domain.services.infra.JwtService

import zio.json.*
import zio.*
import zio.http.*
import zio.json.ast.Json

object WebRTCSignalingServer:
  // WebSocket 消息处理 (带 JWT 认证)
  def websocketHandler(
    sessionId: String,
    clientType: ClientType,
    token: String,
    userId: String
  ): WebSocketApp[SignalingService & JwtService] =
    Handler.webSocket: channel =>
      for
        jwtService <- ZIO.service[JwtService]
        signalingService <- ZIO.service[SignalingService]

//        // 验证 JWT token
//        validationResult <- jwtService.validateToken(token)
//        (tokenSessionId, userId) = validationResult
//
//        // 验证 token 中的 sessionId 是否匹配
//        _ <- ZIO.when(tokenSessionId.value.toString != sessionId):
//          ZIO.fail(new RuntimeException(s"Token sessionId mismatch: expected $sessionId, got ${tokenSessionId.value}"))
//
        // 注册客户端
        _ <- signalingService.registerClient(sessionId, clientType, channel)
        _ <- ZIO.logInfo(s"WebRTC client connected: sessionId=$sessionId, type=$clientType, userId=$userId")

        // 发送 Ready 消息
        _ <- signalingService.relayMessage(
          sessionId,
          clientType,
          SignalingMessage.Ready(sessionId, clientType.toString.toLowerCase)
        )

        // 处理客户端消息
        _ <- channel.receiveAll:
          case ChannelEvent.Read(WebSocketFrame.Text(text)) =>
            parseMessage(text) match
              case Some(message) =>
                // 转发消息到对端
                signalingService.relayMessage(sessionId, clientType, message)
              case None =>
                ZIO.logError(s"Failed to parse message: $text") *>
                  signalingService.relayMessage(
                    sessionId,
                    clientType,
                    SignalingMessage.Error(sessionId, s"Invalid message format", Some("PARSE_ERROR"))
                  )

          case ChannelEvent.Unregistered =>
            signalingService.unregisterClient(sessionId, clientType) *>
              ZIO.logInfo(s"Client disconnected: sessionId=$sessionId, type=$clientType")

          case _ => ZIO.unit
      yield ()
//        ).catchAll: error =>
//        ZIO.logError(s"WebSocket error for session $sessionId: ${error.getMessage}") *>
//          channel.shutdown

  private def parseMessage(text: String): Option[SignalingMessage] =
    text
      .fromJson[Json]
      .toOption
      .flatMap:
        case Json.Obj(fields) =>
          fields
            .collectFirst:
              case ("type", Json.Str("offer")) =>
                text.fromJson[SignalingMessage.Offer].toOption
              case ("type", Json.Str("answer")) =>
                text.fromJson[SignalingMessage.Answer].toOption
              case ("type", Json.Str("ice" | "iceCandidate")) =>
                text.fromJson[SignalingMessage.IceCandidate].toOption
              case ("type", Json.Str("start" | "startSimulation")) =>
                text.fromJson[SignalingMessage.StartSimulation].toOption
              case ("type", Json.Str("stop" | "stopSimulation")) =>
                text.fromJson[SignalingMessage.StopSimulation].toOption
              case ("type", Json.Str("ready")) =>
                text.fromJson[SignalingMessage.Ready].toOption
              case ("type", Json.Str("error")) =>
                text.fromJson[SignalingMessage.Error].toOption
            .flatten
        case _ =>
          None
