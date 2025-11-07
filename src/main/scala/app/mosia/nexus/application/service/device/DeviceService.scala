package app.mosia.nexus.application.service.device

import app.mosia.nexus.application.dto.request.auth.DeviceInfo
import app.mosia.nexus.domain.model.device.{Device, DeviceId}
import app.mosia.nexus.domain.model.user.UserId
import app.mosia.nexus.infra.error.AppTask

trait DeviceService:
  /** 注册或更新设备信息
    *   - 如果设备已存在，更新信息
    *   - 如果设备不存在，创建新设备
    */
  def registerOrUpdateDevice(userId: UserId, deviceInfo: DeviceInfo): AppTask[Device]

  /** 获取用户的所有设备
    */
  def getUserDevices(userId: UserId): AppTask[List[Device]]

  /** 获取设备详情
    */
  def getDevice(deviceId: DeviceId): AppTask[Option[Device]]

  /** 删除设备
    */
  def removeDevice(deviceId: DeviceId): AppTask[Unit]

  /** 删除用户的所有设备（用户注销时）
    */
  def removeAllUserDevices(userId: UserId): AppTask[Unit]

  /** 更新设备的最后活跃时间
    */
  def updateDeviceActivity(deviceId: String): AppTask[Unit]

  /** 更新推送 token
    */
  def updatePushToken(deviceId: String, pushToken: Option[String]): AppTask[Unit]

  /** 检查用户设备数量限制
    */
  def checkDeviceLimit(userId: UserId): AppTask[Boolean]
