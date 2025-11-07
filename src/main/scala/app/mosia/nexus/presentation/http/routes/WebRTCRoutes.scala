package app.mosia.nexus.presentation.http.routes

import app.mosia.nexus.application.service.webRTC.SignalingService
import app.mosia.nexus.application.service.webRTC.WebRTCSignalingServer.websocketHandler
import zio.ZLayer
import zio.http.*

final class WebRTCRoutes:
  val routes: Routes[SignalingService, Response] = Routes(
    // WebSocket 信令端点
    Method.GET / "ws" / "signaling" / string("sessionId") -> handler: (sessionId: String, _: Request) =>
      websocketHandler(sessionId).toResponse
  )

object WebRTCRoutes:
  val live: ZLayer[Any, Nothing, WebRTCRoutes] =
    ZLayer.succeed(new WebRTCRoutes)
