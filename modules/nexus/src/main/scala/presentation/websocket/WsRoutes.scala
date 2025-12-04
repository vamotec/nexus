package app.mosia.nexus
package presentation.websocket

import application.services.WebRTCSignalingServer
import domain.model.resource.ClientType
import domain.services.app.SignalingService
import domain.services.infra.JwtService

import zio.ZIO
import zio.http.*

object WsRoutes:
  // WebSocket Routes (原生 ZIO HTTP，不是 Tapir)
  // 这些路由需要单独注册到 Server
  val routes: Routes[SignalingService & JwtService, Response] = Routes(
    // Frontend WebSocket 信令端点
    // GET /api/webrtc/signaling/frontend/{sessionId}?token=xxx
    Method.GET / "api" / "webrtc" / "signaling" / "frontend" / string("sessionId") -> handler:
      (sessionId: String, request: Request) =>
        request.url.queryParams.getAll("token").collectFirst { case s: String => s } match
          case Some(token) =>
            WebRTCSignalingServer
              .websocketHandler(sessionId, ClientType.Frontend, token, "somue")
              .toResponse // 确保这是一个 ZIO
          case None =>
            ZIO.succeed(Response.badRequest("Missing token parameter"))
    ,

    // Isaac Sim WebSocket 信令端点
    // GET /api/webrtc/signaling/isaac-sim/{sessionId}?token=xxx
    Method.GET / "api" / "webrtc" / "signaling" / "isaac-sim" / string("sessionId") -> handler:
      (sessionId: String, request: Request) =>
        request.url.queryParams.getAll("token").collectFirst { case s: String => s } match
          case Some(token) =>
            WebRTCSignalingServer.websocketHandler(sessionId, ClientType.IsaacSim, token, "somue").toResponse
          case None =>
            ZIO.succeed(Response.badRequest("Missing token parameter"))
  )
