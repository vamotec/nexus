package app.mosia.nexus
package domain.services.app

import domain.model.resource.ClientType
import domain.model.session.SignalingMessage
import zio.json.*
import zio.*
import zio.http.*

trait SignalingService:
  // 注册客户端 (Frontend 或 Isaac Sim)
  def registerClient(sessionId: String, clientType: ClientType, channel: WebSocketChannel): UIO[Unit]

  // 取消注册客户端
  def unregisterClient(sessionId: String, clientType: ClientType): UIO[Unit]

  // 发送消息给 Frontend
  def sendToFrontend(sessionId: String, message: SignalingMessage): UIO[Unit]

  // 发送消息给 Isaac Sim
  def sendToIsaacSim(sessionId: String, message: SignalingMessage): UIO[Unit]

  // 转发信令消息 (自动路由到对端)
  def relayMessage(sessionId: String, fromClientType: ClientType, message: SignalingMessage): UIO[Unit]

  // 检查会话是否已建立 (Frontend 和 Isaac Sim 都已连接)
  def isSessionReady(sessionId: String): UIO[Boolean]
