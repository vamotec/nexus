package app.mosia.nexus
package infrastructure.persistence

import domain.error.*
import domain.model.organization.{OrganizationId, OrganizationMember, OrganizationRole}
import domain.model.user.UserId
import domain.repository.OrganizationMemberRepository
import infrastructure.persistence.BaseSource.PostgresDataSource
import infrastructure.persistence.rows.OrganizationMemberRow

import io.getquill.*
import zio.*

import java.time.Instant
import java.util.UUID
import javax.sql.DataSource

final class OrganizationMemberRepositoryLive(ctx: DefaultDbContext, dataSource: PostgresDataSource)
    extends BaseRepository(ctx, dataSource)
    with OrganizationMemberRepository:

  import ctx.*
  
  private inline def memberSchema = querySchema[OrganizationMemberRow]("organization_members")

  override def add(member: OrganizationMember): AppTask[Unit] = transaction:
    val row = toRow(member)
    run(quote {
      memberSchema.insertValue(lift(row))
    }).unit

  override def update(member: OrganizationMember): AppTask[Unit] = transaction:
    val row = toRow(member)
    run(quote {
      memberSchema
        .filter(m =>
          m.organizationId == lift(row.organizationId) &&
          m.userId == lift(row.userId)
        )
        .updateValue(lift(row))
    }).unit

  override def remove(organizationId: OrganizationId, userId: UserId): AppTask[Long] = transaction:
    val now = Instant.now()
    run(quote {
      memberSchema
        .filter(m =>
          m.organizationId == lift(organizationId.value) &&
          m.userId == lift(userId.value)
        )
        .update(
          _.isActive -> lift(false),
          _.leftAt -> lift(Option(now))
        )
    })

  override def find(organizationId: OrganizationId, userId: UserId): AppTask[Option[OrganizationMember]] = runQuery:
    run(quote {
      memberSchema.filter(m =>
        m.organizationId == lift(organizationId.value) &&
        m.userId == lift(userId.value)
      )
    }).map(_.headOption.map(toDomain))

  override def listByOrganization(organizationId: OrganizationId): AppTask[List[OrganizationMember]] = runQuery:
    run(quote {
      memberSchema.filter(_.organizationId == lift(organizationId.value))
    }).map(_.map(toDomain))

  override def listByUser(userId: UserId): AppTask[List[OrganizationMember]] = runQuery:
    run(quote {
      memberSchema.filter(_.userId == lift(userId.value))
    }).map(_.map(toDomain))

  override def findOwners(organizationId: OrganizationId): AppTask[List[OrganizationMember]] = runQuery:
    run(quote {
      memberSchema.filter(m =>
        m.organizationId == lift(organizationId.value) &&
        m.role == "owner" &&
        m.isActive
      )
    }).map(_.map(toDomain))

  override def countActiveMembers(organizationId: OrganizationId): AppTask[Int] = runQuery:
    run(quote {
      memberSchema
        .filter(m =>
          m.organizationId == lift(organizationId.value) &&
          m.isActive
        )
        .size
    }).map(_.toInt)

  override def countOwnedOrganizations(userId: UserId): AppTask[Int] = runQuery:
    run(quote {
      memberSchema
        .filter(m =>
          m.userId == lift(userId.value) &&
          m.role == "owner" &&
          m.isActive
        )
        .size
    }).map(_.toInt)

  override def updateRole(organizationId: OrganizationId, userId: UserId, role: OrganizationRole): AppTask[Unit] = runQuery:
    ZIO.attempt:
      run(quote {
        memberSchema
          .filter(m =>
            m.organizationId == lift(organizationId.value) &&
            m.userId == lift(userId.value)
          )
          .update(_.role -> lift(role.toString.toLowerCase))
      })

  override def acceptInvite(organizationId: OrganizationId, userId: UserId): AppTask[Unit] = runQuery:
    ZIO.attempt:
      val now = Instant.now()
      run(quote {
        memberSchema
          .filter(m =>
            m.organizationId == lift(organizationId.value) &&
            m.userId == lift(userId.value)
          )
          .update(
            _.isInvited -> lift(false),
            _.joinedAt -> lift(now)
          )
      })

  override def listPendingInvites(userId: UserId): AppTask[List[OrganizationMember]] = runQuery:
    run(quote {
      memberSchema.filter(m =>
        m.userId == lift(userId.value) &&
        m.isInvited &&
        m.isActive
      )
    }).map(_.map(toDomain))

  // ============ 转换方法 ============
  private def toRow(member: OrganizationMember): OrganizationMemberRow =
    OrganizationMemberRow(
      organizationId = member.organizationId.value,
      userId = member.userId.value,
      role = member.role.toString.toLowerCase,
      isActive = member.isActive,
      isInvited = member.isInvited,
      joinedAt = member.joinedAt,
      leftAt = member.leftAt,
      invitedBy = member.invitedBy.map(_.value),
      invitedAt = member.invitedAt
    )

  private def toDomain(row: OrganizationMemberRow): OrganizationMember =
    OrganizationMember(
      organizationId = OrganizationId(row.organizationId),
      userId = UserId(row.userId),
      role = OrganizationRole.fromString(row.role),
      isActive = row.isActive,
      isInvited = row.isInvited,
      joinedAt = row.joinedAt,
      leftAt = row.leftAt,
      invitedBy = row.invitedBy.map(UserId(_)),
      invitedAt = row.invitedAt
    )

object OrganizationMemberRepositoryLive:
  val live: ZLayer[DefaultDbContext & PostgresDataSource, Nothing, OrganizationMemberRepositoryLive] =
    ZLayer.fromFunction(OrganizationMemberRepositoryLive(_, _))
