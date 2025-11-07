package app.mosia.nexus.infra.grpc.neuro.mock

import app.mosia.nexus.domain.model.session.SessionId
import zio.json.JsonCodec

object dto:
  /** gRPC 请求 - 转换为 Protobuf 消息 */
  case class GrpcCreateSessionRequest(
    sessionId: String,
    sceneConfig: GrpcSceneConfig,
    streamConfig: GrpcStreamConfig
  )

  case class GrpcCreateSessionResponse(
    success: Boolean,
    message: String,
    sessionId: SessionId,
    status: GrpcSessionStatusResponse,
    streamEndpoint: GrpcStreamEndpoint
  ) derives JsonCodec

  case class GrpcStreamEndpoint(
    protocol: String,
    url: String,
    port: Int,
    metadate: Map[String, String],
    width: Int,
    height: Int
  ) derives JsonCodec

  case class GrpcSceneConfig(
    robotUrdf: String,
    obstacles: List[GrpcObstacle],
    startPosition: GrpcPosition,
    environment: String
  )

  case class GrpcObstacle(
    obstacleType: Int, // Enum 值
    position: GrpcPosition,
    rotation: GrpcQuaternion,
    size: List[Double]
  )

  case class GrpcPosition(x: Double, y: Double, z: Double)

  case class GrpcQuaternion(w: Double, x: Double, y: Double, z: Double)

  case class GrpcStreamConfig(
    width: Int,
    height: Int,
    fps: Int,
    codec: String,
    bitrate: Int
  )

  /** gRPC 响应 */
  case class GrpcSessionStatusResponse(
    status: Int, // Enum 值
    fps: Double,
    frameCount: Long,
    simulationTime: Double,
    gpuUtilization: Float,
    gpuMemoryMb: Long
  ) derives JsonCodec
