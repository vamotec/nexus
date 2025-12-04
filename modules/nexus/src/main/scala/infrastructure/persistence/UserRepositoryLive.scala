package app.mosia.nexus
package infrastructure.persistence

import domain.error.*
import domain.model.user.UserRole.toRoleStr
import domain.model.user.{Quota, User, UserId, UserRole}
import domain.repository.UserRepository
import infrastructure.persistence.BaseSource.PostgresDataSource
import infrastructure.persistence.rows.{UserQuotaRow, UserRow}

import io.getquill.*
import zio.*

import java.time.Instant
import java.util.UUID

final class UserRepositoryLive(ctx: DefaultDbContext, dataSource: PostgresDataSource)
    extends BaseRepository(ctx, dataSource)
    with UserRepository:

  import ctx.*

  private inline def userSchema  = querySchema[UserRow]("users")
  private inline def quotaSchema = querySchema[UserQuotaRow]("user_quotas")

  override def create(user: User, passwordHash: String): AppTask[Unit] = transaction:
    val userRow = toUserRow(user, passwordHash)
    val quotaRow = toQuotaRow(user)

    for
      _ <- run(quote {
        userSchema.insertValue(lift(userRow))
      })
      _ <- run(quote {
        quotaSchema.insertValue(lift(quotaRow))
      })
    yield ()

  override def updatePassword(userId: UserId, newPasswordHash: String): AppTask[Unit] = transaction:
    run(quote {
      userSchema
        .filter(_.id == lift(userId.value))
        .update(
          _.passwordHash -> lift(newPasswordHash),
          _.updatedAt -> lift(Instant.now())
        )
    }).unit

  override def updatePasswordByEmail(email: String, newPasswordHash: String): AppTask[Unit] = transaction:
    run(quote {
      userSchema
        .filter(_.email == lift(email))
        .update(
          _.passwordHash -> lift(newPasswordHash),
          _.updatedAt -> lift(Instant.now())
        )
    }).unit

  override def markEmailAsVerified(email: String): AppTask[Unit] = transaction:
    run(quote {
      userSchema
        .filter(_.email == lift(email))
        .update(
          _.emailVerified -> lift(true),
          _.isActive -> lift(true),
          _.updatedAt -> lift(Instant.now())
        )
    }).unit

  override def findByEmail(email: String): AppTask[Option[User]] = runQuery:
    run(
      quote {
        for
          u <- userSchema.filter(_.email == lift(email))
          q <- quotaSchema.join(_.userId == u.id)
        yield (u, q)
      }
    ).map(_.headOption.map { case (u, q) =>
      toDomain(u, q)
    })

  override def findById(id: UserId): AppTask[Option[User]] = runQuery:
    run(
      quote {
        for
          u <- userSchema.filter(_.id == lift(id.value))
          q <- quotaSchema.join(_.userId == u.id)
        yield (u, q)
      }
    ).map(_.headOption.map { case (u, q) =>
      toDomain(u, q)
    })

  override def findPasswordHashByEmail(email: String): AppTask[Option[String]] = runQuery:
    run(quote {
      userSchema.filter(_.email == lift(email)).map(_.passwordHash)
    }).map(_.headOption)

  // ============ 转换方法 ============
  private def toUserRow(log: User, passwordHash: String): UserRow =
    UserRow(
      id = log.id.value,
      email = log.email,
      name = log.name,
      avatar = log.avatar,
      passwordHash = passwordHash,
      role = toRoleStr(log.role),
      isActive = log.isActive,
      emailVerified = log.emailVerified,
      createdAt = log.createdAt,
      updatedAt = log.updatedAt,
      lastLoginAt = log.lastLoginAt
    )

  private def toQuotaRow(log: User): UserQuotaRow =
    UserQuotaRow(
      userId = log.id.value,
      maxConcurrentSessions = log.quota.maxConcurrentSessions,
      maxGpuHoursPerMonth = log.quota.maxGpuHoursPerMonth,
      maxStorageGb = log.quota.maxStorageGb,
      maxOwnedOrganizations = log.quota.maxOwnedOrganizations,
      maxJoinedOrganizations = log.quota.maxJoinedOrganizations,
      currentActiveSessions = log.quota.currentActiveSessions,
      currentGpuHoursThisMonth = log.quota.currentGpuHoursThisMonth,
      currentStorageGb = log.quota.currentStorageGb,
      currentOwnedOrganizations = log.quota.currentOwnedOrganizations,
      quotaResetAt = log.quota.quotaResetAt,
      updatedAt = log.updatedAt
    )

  private def toDomain(u: UserRow, q: UserQuotaRow): User =
    User(
      id = UserId(u.id),
      email = u.email,
      name = u.name,
      avatar = u.avatar,
      role = UserRole.fromString(u.role),
      isActive = u.isActive,
      emailVerified = u.emailVerified,
      quota = Quota(
        maxConcurrentSessions = q.maxConcurrentSessions,
        maxGpuHoursPerMonth = q.maxGpuHoursPerMonth,
        maxStorageGb = q.maxStorageGb,
        maxOwnedOrganizations = q.maxOwnedOrganizations,
        maxJoinedOrganizations = q.maxJoinedOrganizations,
        currentActiveSessions = q.currentActiveSessions,
        currentGpuHoursThisMonth = q.currentGpuHoursThisMonth,
        currentStorageGb = q.currentStorageGb,
        currentOwnedOrganizations = q.currentOwnedOrganizations,
        quotaResetAt = q.quotaResetAt
      ),
      createdAt = u.createdAt,
      updatedAt = u.updatedAt,
      lastLoginAt = u.lastLoginAt
    )

object UserRepositoryLive:
  val live: ZLayer[DefaultDbContext & PostgresDataSource, Nothing, UserRepositoryLive] =
    ZLayer.fromFunction(UserRepositoryLive(_, _))
