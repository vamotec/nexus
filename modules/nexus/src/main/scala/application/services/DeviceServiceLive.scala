package app.mosia.nexus
package application.services

import application.dto.request.auth.DeviceInfo
import domain.config.AppConfig
import domain.error.*
import domain.model.device.{Device, DevicePlatform, DeviceUuid}
import domain.model.user.UserId
import domain.repository.DeviceRepository
import domain.services.app.DeviceService

import java.time.Instant

import zio.json.*
import zio.*

class DeviceServiceLive(
  deviceRepo: DeviceRepository,
  config: AppConfig
) extends DeviceService:

  override def registerOrUpdateDevice(
    userId: UserId,
    deviceInfo: DeviceInfo
  ): AppTask[Device] =
    for
      // 1. 解析平台
      platform <- ZIO
        .fromOption(DevicePlatform.fromString(deviceInfo.platform))
        .orElseFail(InvalidDevice("platform", s"${deviceInfo.platform}"))

      // 2. 查找是否已存在该设备
      existingDevice <- deviceRepo.findByUserIdAndDeviceId(userId, deviceInfo.deviceId)

      device <- existingDevice match
        case Some(existing) =>
          // 设备已存在，更新信息
          val updated = existing.copy(
            deviceName = deviceInfo.deviceName,
            platform = platform,
            osVersion = deviceInfo.osVersion,
            appVersion = deviceInfo.appVersion,
            pushToken = deviceInfo.pushToken,
            lastActiveAt = Instant.now(),
            updatedAt = Instant.now()
          )
          deviceRepo.update(updated)
        case None =>
          // 新设备，检查设备数量限制
          for
            _ <- checkDeviceLimit(userId).filterOrFail(identity)(
              InvalidDevice("max devices", config.device.maxDevicesPerUser.toString)
            )
            // 创建新设备
            newDevice = Device(
              id = DeviceUuid.generate(),
              userId = userId,
              deviceId = deviceInfo.deviceId,
              deviceName = deviceInfo.deviceName,
              platform = platform,
              osVersion = deviceInfo.osVersion,
              appVersion = deviceInfo.appVersion,
              pushToken = deviceInfo.pushToken,
              lastActiveAt = Instant.now(),
              createdAt = Instant.now(),
              updatedAt = Instant.now()
            )
            device <- deviceRepo.insert(newDevice)
          yield device
    yield device

  override def getUserDevices(userId: UserId): AppTask[List[Device]] =
    deviceRepo.findByUserId(userId)

  override def getDevice(id: DeviceUuid): AppTask[Option[Device]] =
    deviceRepo.findById(id)

  override def removeDevice(id: DeviceUuid): AppTask[Unit] =
    deviceRepo.delete(id)

  override def removeAllUserDevices(userId: UserId): AppTask[Unit] =
    deviceRepo.deleteByUserId(userId)

  override def updateDeviceActivity(deviceId: String): AppTask[Unit] =
    for
      device <- deviceRepo
        .findByDeviceId(deviceId)
        .someOrFail(NotFound("device", deviceId))
      _ <- deviceRepo.updateLastActive(device.id, Instant.now())
    yield ()

  override def updatePushToken(deviceId: String, pushToken: Option[String]): AppTask[Unit] =
    for
      device <- deviceRepo
        .findByDeviceId(deviceId)
        .someOrFail(NotFound("device", deviceId))
      _ <- deviceRepo.updatePushToken(device.id, pushToken)
    yield ()

  override def checkDeviceLimit(userId: UserId): AppTask[Boolean] =
    for devices <- deviceRepo.findByUserId(userId)
    yield devices.length < config.device.maxDevicesPerUser

object DeviceServiceLive:
  val live: ZLayer[DeviceRepository & AppConfig, Nothing, DeviceService] =
    ZLayer.fromFunction(new DeviceServiceLive(_, _))
