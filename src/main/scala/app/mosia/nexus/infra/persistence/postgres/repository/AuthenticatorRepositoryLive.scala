package app.mosia.nexus.infra.persistence.postgres.repository

import app.mosia.nexus.domain.model.device.DeviceId
import app.mosia.nexus.domain.model.user.AuthenticatorRow
import app.mosia.nexus.domain.repository.AuthenticatorRepository
import app.mosia.nexus.infra.error.AppTask
import zio.ZLayer

import java.time.Instant
import java.util.UUID
import javax.sql.DataSource

class AuthenticatorRepositoryLive(
  ctx: DefaultDbContext,
  dataSource: DataSource
) extends BaseRepository(ctx, dataSource)
    with AuthenticatorRepository:
  import ctx.*

  override def findByDeviceIdAndKeyId(deviceId: DeviceId, keyId: String): AppTask[Option[AuthenticatorRow]] = ???

  override def updateSignCount(id: UUID, newCount: Long): AppTask[Unit] = ???

  override def setLastUsed(id: UUID, when: Instant): AppTask[Unit] = ???

object AuthenticatorRepositoryLive:
  val live: ZLayer[DefaultDbContext & DataSource, Nothing, AuthenticatorRepository] =
    ZLayer.fromFunction(new AuthenticatorRepositoryLive(_, _))
