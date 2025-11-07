package app.mosia.nexus.infra.persistence.postgres.repository

import app.mosia.nexus.domain.model.device.{Device, DeviceId, DevicePlatform}
import app.mosia.nexus.domain.model.user.UserId
import app.mosia.nexus.domain.repository.DeviceRepository
import app.mosia.nexus.infra.error.*
import app.mosia.nexus.infra.persistence.postgres.entity.DeviceEntity
import io.getquill.*
import zio.ZLayer

import java.time.Instant
import javax.sql.DataSource

class DeviceRepositoryLive(ctx: DefaultDbContext, dataSource: DataSource)
    extends BaseRepository(ctx, dataSource)
    with DeviceRepository:
  import ctx.*

  private inline def devices = querySchema[DeviceEntity]("devices")

  override def findById(id: DeviceId): AppTask[Option[Device]] = runQuery:
    run(quote {
      devices.filter(_.id == lift(id.value))
    }).map(_.headOption.map(toDomain))

  override def findByDeviceId(deviceId: String): AppTask[Option[Device]] = runQuery:
    run(quote {
      devices.filter(_.deviceId == lift(deviceId))
    }).map(_.headOption.map(toDomain))

  override def findByUserId(userId: UserId): AppTask[List[Device]] = runQuery:
    run(quote {
      devices
        .filter(_.userId == lift(userId.value))
        .sortBy(_.lastActiveAt)(using Ord.desc)
    }).map(_.map(toDomain))

  override def findByUserIdAndDeviceId(
    userId: UserId,
    deviceId: String
  ): AppTask[Option[Device]] = runQuery:
    run(quote {
      devices
        .filter(d => d.userId == lift(userId.value) && d.deviceId == lift(deviceId))
    }).map(_.headOption.map(toDomain))

  override def insert(device: Device): AppTask[Device] = runQuery:
    val entity = toEntity(device)
    run(quote {
      devices.insertValue(lift(entity)).returning(d => d)
    }).map(toDomain)

  override def update(device: Device): AppTask[Device] = runQuery:
    val entity = toEntity(device)
    run(quote {
      devices
        .filter(_.id == lift(entity.id))
        .updateValue(lift(entity))
        .returning(d => d)
    }).map(toDomain)

  override def delete(id: DeviceId): AppTask[Unit] = runQuery:
    run(quote {
      devices.filter(_.id == lift(id.value)).delete
    }).unit

  override def deleteByUserId(userId: UserId): AppTask[Unit] = runQuery:
    run(quote {
      devices.filter(_.userId == lift(userId.value)).delete
    }).unit

  override def updateLastActive(id: DeviceId, timestamp: Instant): AppTask[Unit] = runQuery:
    run(quote {
      devices
        .filter(_.id == lift(id.value))
        .update(_.lastActiveAt -> lift(timestamp), _.updatedAt -> lift(timestamp))
    }).unit

  override def updatePushToken(id: DeviceId, pushToken: Option[String]): AppTask[Unit] = runQuery:
    run(quote {
      devices
        .filter(_.id == lift(id.value))
        .update(
          _.pushToken -> lift(pushToken),
          _.updatedAt -> lift(Instant.now())
        )
    }).unit

  // ============ 转换方法 ============
  private def toDomain(entity: DeviceEntity): Device =
    Device(
      id = DeviceId(entity.id),
      userId = UserId(entity.userId),
      deviceId = entity.deviceId,
      deviceName = entity.deviceName,
      platform = DevicePlatform
        .fromString(entity.platform)
        .getOrElse(DevicePlatform.Web),
      osVersion = entity.osVersion,
      appVersion = entity.appVersion,
      pushToken = entity.pushToken,
      lastActiveAt = entity.lastActiveAt,
      createdAt = entity.createdAt,
      updatedAt = entity.updatedAt
    )

  private def toEntity(device: Device): DeviceEntity =
    DeviceEntity(
      id = device.id.value,
      userId = device.userId.value,
      deviceId = device.deviceId,
      deviceName = device.deviceName,
      platform = device.platform.toString.toLowerCase,
      osVersion = device.osVersion,
      appVersion = device.appVersion,
      pushToken = device.pushToken,
      lastActiveAt = device.lastActiveAt,
      createdAt = device.createdAt,
      updatedAt = device.updatedAt
    )

object DeviceRepositoryLive:
  val live: ZLayer[DefaultDbContext & DataSource, Nothing, DeviceRepository] =
    ZLayer.fromFunction(new DeviceRepositoryLive(_, _))
