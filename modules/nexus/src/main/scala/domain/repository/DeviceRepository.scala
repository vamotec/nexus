package app.mosia.nexus
package domain.repository

import domain.error.AppTask
import domain.model.device.{Device, DeviceUuid}
import domain.model.user.UserId

import java.time.Instant

trait DeviceRepository:
  def findById(id: DeviceUuid): AppTask[Option[Device]]

  def findByDeviceId(deviceId: String): AppTask[Option[Device]]

  def findByUserId(userId: UserId): AppTask[List[Device]]

  def findByUserIdAndDeviceId(userId: UserId, deviceId: String): AppTask[Option[Device]]

  def insert(device: Device): AppTask[Device]

  def update(device: Device): AppTask[Device]

  def delete(id: DeviceUuid): AppTask[Unit]

  def deleteByUserId(userId: UserId): AppTask[Unit]

  def updateLastActive(id: DeviceUuid, timestamp: Instant): AppTask[Unit]

  def updatePushToken(id: DeviceUuid, pushToken: Option[String]): AppTask[Unit]
