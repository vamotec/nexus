package app.mosia.nexus
package infrastructure.persistence

import domain.error.*
import domain.model.organization.{Organization, OrganizationId, OrganizationQuota, PlanType}
import domain.model.user.UserId
import domain.repository.OrganizationRepository
import infrastructure.persistence.BaseSource.PostgresDataSource
import infrastructure.persistence.rows.{OrganizationMemberRow, OrganizationRow}

import io.getquill.*
import zio.*

import java.time.Instant
import java.util.UUID
import javax.sql.DataSource

final class OrganizationRepositoryLive(ctx: DefaultDbContext, dataSource: PostgresDataSource)
    extends BaseRepository(ctx, dataSource)
    with OrganizationRepository:

  import ctx.*
  
  private inline def orgSchema = querySchema[OrganizationRow]("organizations")
  private inline def memberSchema = querySchema[OrganizationMemberRow]("organization_members")

  override def create(organization: Organization): AppTask[Unit] = transaction:
    val row = toRow(organization)
    run(quote {
      orgSchema.insertValue(lift(row))
    }).unit

  override def findById(id: OrganizationId): AppTask[Option[Organization]] = runQuery:
    run(quote {
      orgSchema.filter(_.id == lift(id.value))
    }).map(_.headOption.map(toDomain))

  override def findByName(name: String): AppTask[Option[Organization]] = runQuery:
    run(quote {
      orgSchema.filter(_.name == lift(name))
    }).map(_.headOption.map(toDomain))

  override def update(organization: Organization): AppTask[Unit] = transaction:
    val row = toRow(organization)
    run(quote {
      orgSchema
        .filter(_.id == lift(row.id))
        .updateValue(lift(row))
    }).unit

  override def softDelete(id: OrganizationId): AppTask[Long] = transaction:
    val now = Instant.now()
    run(quote {
      orgSchema
        .filter(_.id == lift(id.value))
        .update(
          _.isDeleted -> lift(true),
          _.isActive -> lift(false),
          _.deletedAt -> lift(Option(now)),
          _.updatedAt -> lift(now)
        )
    })

  override def listByOwner(userId: UserId): AppTask[List[Organization]] = runQuery:
    run(quote {
      for
        m <- memberSchema.filter(m => m.userId == lift(userId.value) && m.role == "owner" && m.isActive)
        o <- orgSchema.join(_.id == m.organizationId).filter(!_.isDeleted)
      yield o
    }).map(_.map(toDomain))

  override def listByMember(userId: UserId): AppTask[List[Organization]] = runQuery:
    run(quote {
      for
        m <- memberSchema.filter(m => m.userId == lift(userId.value) && m.isActive)
        o <- orgSchema.join(_.id == m.organizationId).filter(!_.isDeleted)
      yield o
    }).map(_.map(toDomain))

  override def countByOwner(userId: UserId): AppTask[Int] = runQuery:
    run(quote {
      memberSchema
        .filter(m => m.userId == lift(userId.value) && m.role == "owner" && m.isActive)
        .size
    }).map(_.toInt)

  override def countMembers(organizationId: OrganizationId): AppTask[Int] = runQuery:
    run(quote {
      memberSchema
        .filter(m => m.organizationId == lift(organizationId.value) && m.isActive)
        .size
    }).map(_.toInt)

  override def existsByName(name: String): AppTask[Boolean] = runQuery:
    run(quote {
      orgSchema.filter(_.name == lift(name)).nonEmpty
    })

  override def isMember(organizationId: OrganizationId, userId: UserId): AppTask[Boolean] = runQuery:
    run(quote {
      memberSchema
        .filter(m =>
          m.organizationId == lift(organizationId.value) &&
          m.userId == lift(userId.value) &&
          m.isActive
        )
        .nonEmpty
    })

  // ============ 转换方法 ============
  private def toRow(org: Organization): OrganizationRow =
    OrganizationRow(
      id = org.id.value,
      name = org.name,
      description = org.description,
      avatar = org.avatar,
      planType = org.planType.toString.toLowerCase,
      maxUsers = org.quota.maxUsers,
      maxStorageGb = org.quota.maxStorageGb,
      maxGpuHoursPerMonth = org.quota.maxGpuHoursPerMonth,
      isActive = org.isActive,
      isDeleted = org.isDeleted,
      createdAt = org.createdAt,
      updatedAt = org.updatedAt,
      deletedAt = org.deletedAt
    )

  private def toDomain(row: OrganizationRow): Organization =
    Organization(
      id = OrganizationId(row.id),
      name = row.name,
      description = row.description,
      avatar = row.avatar,
      planType = PlanType.fromString(row.planType),
      quota = OrganizationQuota(
        maxUsers = row.maxUsers,
        maxStorageGb = row.maxStorageGb,
        maxGpuHoursPerMonth = row.maxGpuHoursPerMonth
      ),
      isActive = row.isActive,
      isDeleted = row.isDeleted,
      createdAt = row.createdAt,
      updatedAt = row.updatedAt,
      deletedAt = row.deletedAt
    )

object OrganizationRepositoryLive:
  val live: ZLayer[DefaultDbContext & PostgresDataSource, Nothing, OrganizationRepositoryLive] =
    ZLayer.fromFunction(OrganizationRepositoryLive(_, _))
