package app.mosia.nexus
package domain.model.session

import zio.json.*
import zio.*

sealed trait SignalingMessage

object SignalingMessage:
  // WebRTC 信令消息
  case class Offer(sessionId: String, sdp: String) extends SignalingMessage
  case class Answer(sessionId: String, sdp: String) extends SignalingMessage
  case class IceCandidate(
    sessionId: String,
    candidate: String,
    sdpMid: String,
    sdpMLineIndex: Int
  ) extends SignalingMessage

  // 控制消息
  case class StartSimulation(sessionId: String, sceneConfig: String) extends SignalingMessage
  case class StopSimulation(sessionId: String) extends SignalingMessage

  // 连接状态消息
  case class Ready(sessionId: String, clientType: String)
      extends SignalingMessage // clientType: "frontend" | "isaac-sim"
  case class Error(sessionId: String, message: String, code: Option[String] = None) extends SignalingMessage

  // JSON 编解码器
  given JsonCodec[Offer]           = DeriveJsonCodec.gen[Offer]
  given JsonCodec[Answer]          = DeriveJsonCodec.gen[Answer]
  given JsonCodec[IceCandidate]    = DeriveJsonCodec.gen[IceCandidate]
  given JsonCodec[StartSimulation] = DeriveJsonCodec.gen[StartSimulation]
  given JsonCodec[StopSimulation]  = DeriveJsonCodec.gen[StopSimulation]
  given JsonCodec[Ready]           = DeriveJsonCodec.gen[Ready]
  given JsonCodec[Error]           = DeriveJsonCodec.gen[Error]
