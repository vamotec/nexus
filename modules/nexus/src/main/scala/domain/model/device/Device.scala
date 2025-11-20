package app.mosia.nexus
package domain.model.device

import domain.model.user.UserId

import java.time.Instant

case class Device(
  id: DeviceUuid,
  userId: UserId,
  deviceId: String, // 设备唯一标识（如 iOS IDFA, Android ID）
  deviceName: String, // 设备名称（如 "John's iPhone"）
  platform: DevicePlatform, // ios, android, web
  osVersion: String, // 操作系统版本
  appVersion: String, // 应用版本
  pushToken: Option[String], // FCM/APNs token
  lastActiveAt: Instant, // 最后活跃时间
  createdAt: Instant,
  updatedAt: Instant
)
