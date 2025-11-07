package app.mosia.nexus.application.service.device

import app.mosia.nexus.application.dto.request.auth.DeviceInfo
import app.mosia.nexus.application.service.audit.{AuditService, AuditServiceLive}
import app.mosia.nexus.domain.model.device.{Device, DeviceId, DevicePlatform}
import app.mosia.nexus.domain.model.user.UserId
import app.mosia.nexus.domain.repository.{AuditLogRepository, DeviceRepository}
import app.mosia.nexus.infra.config.DeviceServiceConfig
import app.mosia.nexus.infra.error.{AppTask, DeviceError}
import zio.{ZIO, ZLayer}

import java.time.Instant

class DeviceServiceLive(
  deviceRepo: DeviceRepository,
  config: DeviceServiceConfig
) extends DeviceService:

  override def registerOrUpdateDevice(
    userId: UserId,
    deviceInfo: DeviceInfo
  ): AppTask[Device] =
    for
      // 1. 解析平台
      platform <- ZIO
        .fromOption(DevicePlatform.fromString(deviceInfo.platform))
        .orElseFail(DeviceError.InvalidDeviceData(s"Invalid platform: ${deviceInfo.platform}"))

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
              DeviceError.DeviceLimitExceeded(config.maxDevicesPerUser)
            )
            // 创建新设备
            newDevice = Device(
              id = DeviceId.generate(),
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

  override def getDevice(deviceId: DeviceId): AppTask[Option[Device]] =
    deviceRepo.findById(deviceId)

  override def removeDevice(deviceId: DeviceId): AppTask[Unit] =
    deviceRepo.delete(deviceId)

  override def removeAllUserDevices(userId: UserId): AppTask[Unit] =
    deviceRepo.deleteByUserId(userId)

  override def updateDeviceActivity(deviceId: String): AppTask[Unit] =
    for
      device <- deviceRepo
        .findByDeviceId(deviceId)
        .someOrFail(DeviceError.DeviceNotFound)
      _ <- deviceRepo.updateLastActive(device.id, Instant.now())
    yield ()

  override def updatePushToken(deviceId: String, pushToken: Option[String]): AppTask[Unit] =
    for
      device <- deviceRepo
        .findByDeviceId(deviceId)
        .someOrFail(DeviceError.DeviceNotFound)
      _ <- deviceRepo.updatePushToken(device.id, pushToken)
    yield ()

  override def checkDeviceLimit(userId: UserId): AppTask[Boolean] =
    for devices <- deviceRepo.findByUserId(userId)
    yield devices.length < config.maxDevicesPerUser

object DeviceServiceLive:
//  val live: ZLayer[DeviceRepository, Nothing, DeviceService] =
//    ZLayer.fromFunction { (repository: DeviceRepository) =>
//      new DeviceServiceLive(
//        repository,
//        DeviceServiceConfig() // 使用默认配置
//      )
//    }
//
//  def layerWithConfig(config: DeviceServiceConfig): ZLayer[DeviceRepository, Nothing, DeviceService] =
//    ZLayer.fromFunction { (repository: DeviceRepository) =>
//      new DeviceServiceLive(repository, config)
//    }

  val live: ZLayer[DeviceRepository & DeviceServiceConfig, Nothing, DeviceServiceLive] =
    ZLayer.fromFunction(new DeviceServiceLive(_, _))
