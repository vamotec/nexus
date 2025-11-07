package app.mosia.nexus.infra.persistence.postgres.entity

import java.time.Instant
import java.util.UUID

case class DeviceEntity(
  id: UUID,
  userId: UUID,
  deviceId: String,
  deviceName: String,
  platform: String,
  osVersion: String,
  appVersion: String,
  pushToken: Option[String],
  lastActiveAt: Instant,
  createdAt: Instant,
  updatedAt: Instant
)
