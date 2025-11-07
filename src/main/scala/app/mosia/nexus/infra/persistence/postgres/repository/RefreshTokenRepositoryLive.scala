package app.mosia.nexus.infra.persistence.postgres.repository

import app.mosia.nexus.domain.model.user.RefreshToken
import app.mosia.nexus.domain.repository.RefreshTokenRepository
import app.mosia.nexus.infra.error.AppTask
import zio.ZLayer

import javax.sql.DataSource

final class RefreshTokenRepositoryLive(ctx: DefaultDbContext, dataSource: DataSource)
    extends BaseRepository(ctx, dataSource)
    with RefreshTokenRepository:
  import ctx.*

  override def save(refreshToken: RefreshToken): AppTask[Unit] = ???

  override def findByToken(token: String): AppTask[Option[RefreshToken]] = ???

  override def markAsRevoked(token: String): AppTask[Unit] = ???

  override def delete(token: String): AppTask[Unit] = ???

object RefreshTokenRepositoryLive:
  val live: ZLayer[DefaultDbContext & DataSource, Nothing, RefreshTokenRepository] =
    ZLayer.fromFunction(new RefreshTokenRepositoryLive(_, _))
