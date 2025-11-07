package app.mosia.nexus.domain.repository

import app.mosia.nexus.domain.model.device.DeviceId
import app.mosia.nexus.domain.model.user.AuthenticatorRow
import app.mosia.nexus.infra.error.AppTask

import java.time.Instant
import java.util.UUID

trait AuthenticatorRepository:
  def findByDeviceIdAndKeyId(deviceId: DeviceId, keyId: String): AppTask[Option[AuthenticatorRow]]

  def updateSignCount(id: UUID, newCount: Long): AppTask[Unit]

  def setLastUsed(id: UUID, when: Instant): AppTask[Unit]
