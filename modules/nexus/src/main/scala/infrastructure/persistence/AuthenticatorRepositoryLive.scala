package app.mosia.nexus
package infrastructure.persistence

import domain.error.AppTask
import domain.model.user.{Authenticator, UserId}
import domain.repository.AuthenticatorRepository
import infrastructure.persistence.BaseSource.PostgresDataSource
import infrastructure.persistence.rows.AuthenticatorRow

import io.getquill.*
import zio.*

import java.time.Instant
import java.util.UUID
import javax.sql.DataSource

class AuthenticatorRepositoryLive(
  ctx: DefaultDbContext,
  dataSource: PostgresDataSource
) extends BaseRepository(ctx, dataSource)
    with AuthenticatorRepository:
  
  import ctx.*
  
  private inline def authenticatorSchema = querySchema[AuthenticatorRow]("authenticators")

  override def findByDeviceIdAndKeyId(deviceId: String, keyId: String): AppTask[Option[Authenticator]] = runQuery:
    run(quote {
      authenticatorSchema
        .filter(a => a.deviceId == lift(deviceId) && a.keyId == lift(keyId))
    }).map(_.headOption.map(toAuthenticator))

  override def updateSignCount(id: UUID, newCount: Option[Long]): AppTask[Unit] = transaction:
    ZIO.attempt:
      run(quote {
        authenticatorSchema
          .filter(_.id == lift(id))
          .update(_.signCount -> lift(newCount))
      })

  override def setLastUsed(id: UUID, when: Instant): AppTask[Unit] = transaction:
    ZIO.attempt:
      run(quote {
        authenticatorSchema
          .filter(_.id == lift(id))
          .update(_.lastUsedAt -> lift(Option(when)))
      })

  // ============ 转换方法 ============
  private def toAuthenticator(entity: AuthenticatorRow): Authenticator =
    Authenticator(
      id = entity.id,
      userId = UserId(entity.userId),
      deviceId = entity.deviceId,
      keyId = entity.keyId,
      publicKey = entity.publicKey,
      signCount = entity.signCount,
      lastUsedAt = entity.lastUsedAt
    )

object AuthenticatorRepositoryLive:
  val live: ZLayer[DefaultDbContext & PostgresDataSource, Nothing, AuthenticatorRepository] =
    ZLayer.fromFunction(AuthenticatorRepositoryLive(_, _))
