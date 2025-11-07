package app.mosia.nexus.domain.model.session

import zio.json.{DeriveJsonCodec, JsonCodec}

sealed trait SignalingMessage
object SignalingMessage:
  case class Offer(sessionId: String, sdp: String) extends SignalingMessage
  case class Answer(sessionId: String, sdp: String) extends SignalingMessage
  case class IceCandidate(
    sessionId: String,
    candidate: String,
    sdpMid: String,
    sdpMLineIndex: Int
  ) extends SignalingMessage
  case class StartSimulation(sessionId: String, sceneConfig: String) extends SignalingMessage
  case class StopSimulation(sessionId: String) extends SignalingMessage

  // JSON 编解码器
  given JsonCodec[Offer]           = DeriveJsonCodec.gen[Offer]
  given JsonCodec[Answer]          = DeriveJsonCodec.gen[Answer]
  given JsonCodec[IceCandidate]    = DeriveJsonCodec.gen[IceCandidate]
  given JsonCodec[StartSimulation] = DeriveJsonCodec.gen[StartSimulation]
  given JsonCodec[StopSimulation]  = DeriveJsonCodec.gen[StopSimulation]
