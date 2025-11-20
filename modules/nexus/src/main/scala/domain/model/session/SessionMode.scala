package app.mosia.nexus
package domain.model.session

import domain.model.common.ValueObject
import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

/** 会话模式枚举
  *
  * 定义了三种不同的仿真会话模式，每种模式有不同的资源分配和控制方式
  */
enum SessionMode extends ValueObject:
  /** 手动控制模式
    *
    *   - 仅分配 Isaac Sim 实例
    *   - 需要 WebRTC 视频流
    *   - 用户通过 WebSocket 手动控制机器人
    *   - 适用场景：手动测试、演示、数据收集
    */
  case Manual

  /** AI 训练模式
    *
    *   - 分配 Training Instance + 多个 Isaac Sim 实例（并行环境）
    *   - 不需要 WebRTC 视频流
    *   - AI 通过 ZMQ 与 Isaac Sim 通信
    *   - 适用场景：强化学习训练、批量仿真
    */
  case Training

  /** 混合模式
    *
    *   - 分配 Training Instance + 多个 Isaac Sim 实例
    *   - 需要 WebRTC 视频流
    *   - 支持在 AI 控制和人工控制间切换
    *   - 适用场景：人工演示、AI 行为纠正、协作训练
    */
  case Hybrid

object SessionMode:
  /** 从字符串解析 SessionMode */
  def fromString(s: String): SessionMode =
    s.toLowerCase match
      case "manual" => Manual
      case "training" => Training
      case "hybrid" => Hybrid
      case _ => throw new IllegalArgumentException(s"Invalid SessionMode: $s")

  /** 判断是否需要 WebRTC 连接 */
  extension (mode: SessionMode)
    def needsWebRTC: Boolean = mode match
      case Manual | Hybrid => true
      case Training => false

    /** 判断是否需要 AI 训练资源 */
    def needsTrainingResources: Boolean = mode match
      case Training | Hybrid => true
      case Manual => false

    /** 判断是否支持人工控制 */
    def supportsManualControl: Boolean = mode match
      case Manual | Hybrid => true
      case Training => false

    /** 转换为 gRPC SessionMode */
    def toGrpcMode: domain.grpc.neuro.SessionMode = mode match
      case Manual => domain.grpc.neuro.SessionMode.MANUAL
      case Training => domain.grpc.neuro.SessionMode.TRAINING
      case Hybrid => domain.grpc.neuro.SessionMode.HYBRID
