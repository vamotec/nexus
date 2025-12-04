package app.mosia.nexus
package infrastructure.persistence

import domain.error.*
import domain.model.device.{Device, DevicePlatform, DeviceUuid}
import domain.model.user.UserId
import domain.repository.DeviceRepository
import infrastructure.persistence.BaseSource.PostgresDataSource
import infrastructure.persistence.rows.DeviceRow

import io.getquill.*
import zio.*

import java.time.Instant
import javax.sql.DataSource

class DeviceRepositoryLive(ctx: DefaultDbContext, dataSource: PostgresDataSource)
    extends BaseRepository(ctx, dataSource)
    with DeviceRepository:

  import ctx.*
  
  private inline def devices = querySchema[DeviceRow]("devices")

  override def findById(id: DeviceUuid): AppTask[Option[Device]] = runQuery:
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
    val entity = toRow(device)
    run(quote {
      devices.insertValue(lift(entity)).returning(d => d)
    }).map(toDomain)

  override def update(device: Device): AppTask[Device] = runQuery:
    val entity = toRow(device)
    run(quote {
      devices
        .filter(_.id == lift(entity.id))
        .updateValue(lift(entity))
        .returning(d => d)
    }).map(toDomain)

  override def delete(id: DeviceUuid): AppTask[Unit] = runQuery:
    run(quote {
      devices.filter(_.id == lift(id.value)).delete
    }).unit

  override def deleteByUserId(userId: UserId): AppTask[Unit] = runQuery:
    run(quote {
      devices.filter(_.userId == lift(userId.value)).delete
    }).unit

  override def updateLastActive(id: DeviceUuid, timestamp: Instant): AppTask[Unit] = runQuery:
    run(quote {
      devices
        .filter(_.id == lift(id.value))
        .update(_.lastActiveAt -> lift(timestamp), _.updatedAt -> lift(timestamp))
    }).unit

  override def updatePushToken(id: DeviceUuid, pushToken: Option[String]): AppTask[Unit] = runQuery:
    run(quote {
      devices
        .filter(_.id == lift(id.value))
        .update(
          _.pushToken -> lift(pushToken),
          _.updatedAt -> lift(Instant.now())
        )
    }).unit

  // ============ 转换方法 ============
  private def toDomain(entity: DeviceRow): Device =
    Device(
      id = DeviceUuid(entity.id),
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

  private def toRow(device: Device): DeviceRow =
    DeviceRow(
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
  val live: ZLayer[DefaultDbContext & PostgresDataSource, Nothing, DeviceRepository] =
    ZLayer.fromFunction(DeviceRepositoryLive(_, _))
