package app.mosia.nexus.infra.persistence.postgres.repository

import app.mosia.nexus.domain.model.user.{User, UserId}
import app.mosia.nexus.domain.repository.UserRepository
import app.mosia.nexus.infra.error.*
import app.mosia.nexus.infra.persistence.QuillUserCodecs
import app.mosia.nexus.infra.persistence.postgres.entity.{UserAuthRecord, UserEntity, UserQuotaEntity}
import io.getquill.*
import zio.{IO, Task, ULayer, ZIO, ZLayer}

import java.sql.SQLException
import java.time.Instant
import java.util.UUID
import javax.sql.DataSource

final class UserRepositoryLive(ctx: DefaultDbContext, dataSource: DataSource)
    extends BaseRepository(ctx, dataSource)
    with QuillUserCodecs
    with UserRepository:
  import ctx.*

  private inline def userSchema  = querySchema[UserEntity]("users")
  private inline def quotaSchema = querySchema[UserQuotaEntity]("user_quotas")

  override def create(user: User, passwordHash: String): AppTask[User] = transaction:
    ZIO.attempt:
      val userEntity = toUserEntity(user, passwordHash)
      run(quote {
        userSchema.insertValue(lift(userEntity))
      })
      val quotaEntity = toQuotaEntity(user)
      run(quote {
        quotaSchema.insertValue(lift(quotaEntity))
      })
      user

  override def updatePassword(userId: UUID, newPasswordHash: String): AppTask[Unit] = runQuery:
    ZIO.attempt:
      run(quote {
        userSchema
          .filter(_.id == lift(userId))
          .update(
            _.passwordHash -> lift(newPasswordHash),
            _.updatedAt -> lift(Instant.now())
          )
      })

  override def findByEmail(email: String): AppTask[Option[User]] = runQuery:
    run(
      quote {
        for
          u <- userSchema.filter(_.email == lift(email))
          q <- quotaSchema.join(_.userId == u.id)
        yield (u, q)
      }
    ).map(_.headOption.map { case (u, q) =>
      User.fromEntities(u, q)
    })

  override def findById(id: UUID): AppTask[Option[User]] = runQuery:
    run(
      quote {
        for
          u <- userSchema.filter(_.id == lift(id))
          q <- quotaSchema.join(_.userId == u.id)
        yield (u, q)
      }
    ).map(_.headOption.map { case (u, q) =>
      User.fromEntities(u, q)
    })

  override def findPasswordHashByEmail(email: String): AppTask[Option[String]] = runQuery:
    run(quote {
      userSchema.filter(_.email == lift(email)).map(_.passwordHash)
    }).map(_.headOption)

  // ============ 转换方法 ============
  private def toUserEntity(log: User, passwordHash: String): UserEntity =
    UserEntity(
      id = log.id.value,
      email = log.email,
      name = log.name,
      avatar = log.avatar,
      passwordHash = passwordHash,
      organization = log.organization,
      role = log.role,
      isActive = log.isActive,
      emailVerified = log.emailVerified,
      createdAt = log.createdAt,
      updatedAt = log.updatedAt,
      lastLoginAt = log.lastLoginAt
    )

  private def toQuotaEntity(log: User): UserQuotaEntity =
    UserQuotaEntity(
      userId = log.id.value,
      maxConcurrentSessions = log.quota.maxConcurrentSessions,
      maxGpuHoursPerMonth = log.quota.maxGpuHoursPerMonth,
      maxStorageGb = log.quota.maxStorageGb,
      currentActiveSessions = log.quota.currentActiveSessions,
      currentGpuHoursThisMonth = log.quota.currentGpuHoursThisMonth,
      currentStorageGb = log.quota.currentStorageGb,
      quotaResetAt = log.quota.quotaResetAt,
      updatedAt = log.updatedAt
    )

object UserRepositoryLive:
  val live: ZLayer[DefaultDbContext & DataSource, Nothing, UserRepositoryLive] =
    ZLayer.fromFunction(UserRepositoryLive(_, _))
