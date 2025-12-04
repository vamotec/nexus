package app.mosia.nexus
package infrastructure.persistence

import domain.error.AppTask
import domain.model.user.{Challenge, UserId}
import domain.repository.ChallengeRepository
import infrastructure.persistence.BaseSource.PostgresDataSource
import infrastructure.persistence.rows.AuthChallengeRow

import io.getquill.*
import io.getquill.extras.InstantOps
import zio.*

import java.security.SecureRandom
import java.time.Instant
import java.util.{Base64, UUID}
import javax.sql.DataSource

class ChallengeRepositoryLive(ctx: DefaultDbContext, dataSource: PostgresDataSource)
    extends BaseRepository(ctx, dataSource)
    with ChallengeRepository:

  import ctx.*
  
  private inline def challengeSchema = querySchema[AuthChallengeRow]("auth_challenges")

  override def createChallenge(
    userId: Option[UserId],
    deviceId: Option[String],
    purpose: String,
    ttlSeconds: Long
  ): AppTask[Challenge] = transaction:
    for
      now <- Clock.instant
      challengeStr <- ZIO.attempt(generateRandomChallenge())
      id        = UUID.randomUUID()
      expiresAt = now.plusSeconds(ttlSeconds)
      entity    = AuthChallengeRow(
        id = id,
        userId = userId.map(_.value),
        deviceId = deviceId,
        challenge = challengeStr,
        purpose = purpose,
        expiresAt = expiresAt,
        consumed = Some(false),
        createdAt = Some(now)
      )
      _ <- run(quote {
        challengeSchema.insertValue(lift(entity))
      })
    yield toChallenge(entity)

  override def consumeChallenge(challenge: String): AppTask[Option[Challenge]] = transaction:
    for
      now <- Clock.instant
      // 原子性地查找并标记为已消费
      result <- run(quote {
        challengeSchema
          .filter(c =>
            c.challenge == lift(challenge) &&
              c.expiresAt > lift(now) &&
              c.consumed == lift(Option(false))
          )
      }).map(_.headOption)
      _ <- result match
        case Some(entity) =>
          run(quote {
            challengeSchema
              .filter(_.id == lift(entity.id))
              .update(_.consumed -> lift(Option(true)))
          })
        case None => ZIO.unit
    yield result.map(toChallenge)

  override def findValidChallenge(challenge: String): AppTask[Option[Challenge]] = runQuery:
    for
      now <- Clock.instant
      result <-
        run(quote {
          challengeSchema
            .filter(c =>
              c.challenge == lift(challenge) &&
                c.expiresAt > lift(now) &&
                c.consumed == lift(Option(false))
            )
        }).map(_.headOption)
    yield result.map(toChallenge)

  // ============ 转换方法 ============
  private def toChallenge(entity: AuthChallengeRow): Challenge =
    Challenge(
      id = entity.id,
      userId = entity.userId.map(UserId(_)),
      deviceId = entity.deviceId,
      challenge = entity.challenge,
      purpose = entity.purpose,
      expiresAt = entity.expiresAt,
      consumed = entity.consumed
    )

  // ============ 辅助方法 ============
  private def generateRandomChallenge(): String =
    val random = new SecureRandom()
    val bytes  = new Array[Byte](32)
    random.nextBytes(bytes)
    Base64.getUrlEncoder.withoutPadding().encodeToString(bytes)

object ChallengeRepositoryLive:
  val live: ZLayer[DefaultDbContext & PostgresDataSource, Nothing, ChallengeRepository] =
    ZLayer.fromFunction(ChallengeRepositoryLive(_, _))
