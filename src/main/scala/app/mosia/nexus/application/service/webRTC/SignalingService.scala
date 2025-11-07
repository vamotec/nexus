package app.mosia.nexus.application.service.webRTC

import app.mosia.nexus.domain.model.session.SignalingMessage
import zio.UIO
import zio.http.WebSocketChannel

trait SignalingService:
  def registerClient(sessionId: String, channel: WebSocketChannel): UIO[Unit]

  def unregisterClient(sessionId: String): UIO[Unit]

  def sendToClient(sessionId: String, message: SignalingMessage): UIO[Unit]

  def broadcastMessage(message: SignalingMessage): UIO[Unit]
