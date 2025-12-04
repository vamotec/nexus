package app.mosia.nexus
package presentation.http.v1

import application.dto.model.webRTC.WebRTCConnectionInfo
import application.dto.response.common.ApiResponse
import domain.error.*
import domain.model.jwt.Payload
import domain.model.jwt.Permission.{Admin, Editor}
import domain.model.jwt.TokenType.Control
import domain.model.resource.ClientType
import domain.model.session.SessionId
import domain.services.app.{SessionService, SignalingService}
import domain.services.infra.{JwtContent, JwtService}
import app.mosia.nexus.presentation.http.EndpointModule
import app.mosia.nexus.presentation.http.SecureEndpoints.baseEndpoint

import sttp.capabilities.zio.ZioStreams
import sttp.model.StatusCode
import sttp.tapir.json.zio.jsonBody
import sttp.tapir.ztapir.*
import sttp.tapir.{Endpoint, EndpointOutput}
import zio.ZIO

final class WebRTCEndpoint(
  sessionService: SessionService,
  signalingService: SignalingService
) extends EndpointModule:

  override def publicEndpoints: List[ZServerEndpoint[Any, ZioStreams]] = List.empty

  override def secureEndpoints: List[ZServerEndpoint[JwtContent, ZioStreams]] =
    List(
      getConnectionInfo
    )

  // 获取 WebRTC 连接信息 (REST endpoint)
  // GET /api/webrtc/connection/{sessionId}?clientType=frontend|isaac-sim
  private val getConnectionInfo: ZServerEndpoint[JwtContent, ZioStreams] =
    baseEndpoint.get
      .in("webrtc" / "connection" / path[String]("sessionId"))
      .in(query[String]("clientType"))
      .out(jsonBody[ApiResponse[WebRTCConnectionInfo]])
      .zServerLogic { case (sessionId, clientTypeStr) =>
        (for
          userIdOpt <- ZIO.serviceWithZIO[JwtContent](_.get)
          userId <- ZIO.fromOption(userIdOpt).mapError(_ => NotFound("content", "userId"))
          // 验证 clientType 参数
          clientType <- clientTypeStr.toLowerCase match
            case "frontend" => ZIO.succeed(ClientType.Frontend)
            case "isaac-sim" | "isaacSim" => ZIO.succeed(ClientType.IsaacSim)
            case _ => ZIO.fail(InvalidInput("webrtc client type", clientTypeStr))

          // 生成 control token
          token <- sessionService.generateControlToken(userId, sessionId)

          // 构建 WebSocket URL
          // 格式: ws://localhost:8080/api/webrtc/signaling/{clientType}/{sessionId}?token=xxx
          webrtcPath = clientType match
            case ClientType.Frontend => s"/api/webrtc/signaling/frontend/$sessionId"
            case ClientType.IsaacSim => s"/api/webrtc/signaling/isaac-sim/$sessionId"

          connectionInfo = WebRTCConnectionInfo(
            sessionId = sessionId,
            webrtcSignalingUrl = webrtcPath, // 前端需要自行添加协议和主机
            token = token,
            clientType = clientType.toString.toLowerCase
          )

          response = ApiResponse(connectionInfo)
        yield response)
          .mapError(toErrorResponse)
      }
