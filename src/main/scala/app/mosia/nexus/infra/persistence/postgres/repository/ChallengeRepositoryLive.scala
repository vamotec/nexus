package app.mosia.nexus.infra.persistence.postgres.repository

import app.mosia.nexus.domain.model.device.DeviceId
import app.mosia.nexus.domain.model.user.{ChallengeRow, UserId}
import app.mosia.nexus.domain.repository.ChallengeRepository
import app.mosia.nexus.infra.error.AppTask
import zio.ZLayer

import javax.sql.DataSource

class ChallengeRepositoryLive(ctx: DefaultDbContext, dataSource: DataSource)
    extends BaseRepository(ctx, dataSource)
    with ChallengeRepository:
  import ctx.*

  override def createChallenge(
    userId: Option[UserId],
    deviceId: Option[DeviceId],
    purpose: String,
    ttlSeconds: Long
  ): AppTask[ChallengeRow] = ???

  override def consumeChallenge(challenge: String): AppTask[Option[ChallengeRow]] = ???

  override def findValidChallenge(challenge: String): AppTask[Option[ChallengeRow]] = ???

object ChallengeRepositoryLive:
  val live: ZLayer[DefaultDbContext & DataSource, Nothing, ChallengeRepository] =
    ZLayer.fromFunction(new ChallengeRepositoryLive(_, _))
