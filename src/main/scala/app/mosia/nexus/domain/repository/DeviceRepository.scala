package app.mosia.nexus.domain.repository

import app.mosia.nexus.domain.model.device.{Device, DeviceId}
import app.mosia.nexus.domain.model.user.UserId
import app.mosia.nexus.infra.error.AppTask

import java.time.Instant

trait DeviceRepository:
  def findById(id: DeviceId): AppTask[Option[Device]]

  def findByDeviceId(deviceId: String): AppTask[Option[Device]]

  def findByUserId(userId: UserId): AppTask[List[Device]]

  def findByUserIdAndDeviceId(userId: UserId, deviceId: String): AppTask[Option[Device]]

  def insert(device: Device): AppTask[Device]

  def update(device: Device): AppTask[Device]

  def delete(id: DeviceId): AppTask[Unit]

  def deleteByUserId(userId: UserId): AppTask[Unit]

  def updateLastActive(id: DeviceId, timestamp: Instant): AppTask[Unit]

  def updatePushToken(id: DeviceId, pushToken: Option[String]): AppTask[Unit]
