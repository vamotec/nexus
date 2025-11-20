package app.mosia.nexus
package infrastructure.persistence.postgres.repository

import infrastructure.persistence.BaseSource.PostgresDataSource
import infrastructure.persistence.postgres.rows.ProjectRow
import domain.error.*
import domain.model.project.*
import domain.model.user.UserId
import domain.repository.ProjectRepository

import zio.*

import java.util.UUID
import io.getquill.*
import io.getquill.context.json.*

final class ProjectRepositoryLive(ctx: DefaultDbContext, dataSource: PostgresDataSource)
    extends BaseRepository(ctx, dataSource)
    with ProjectRepository:

  import ctx.*
  private inline def projectSchema = querySchema[ProjectRow]("projects")

  override def save(project: Project): AppTask[Unit] = transaction:
    val row = toRow(project)
    run(quote {
      projectSchema.insertValue(lift(row))
    }).unit

//  override def save(project: Project): AppTask[Unit] =
//    withTransaction { conn =>
//      val row = toRow(project)
//      val stmt = conn.prepareStatement(
//        """INSERT INTO projects
//           (id, name, description, owner_id, tags, settings, state, created_at, updated_at)
//           VALUES (?, ?, ?, ?, ?::jsonb, ?::jsonb, ?, ?, ?)"""
//      )
//
//      stmt.setObject(1, row.id)
//      stmt.setString(2, row.name)
//      stmt.setString(3, row.description.orNull)
//      stmt.setObject(4, row.ownerId)
//      stmt.setString(5, row.tags)
//      stmt.setString(6, row.settings)
//      stmt.setTimestamp(8, java.sql.Timestamp.from(row.createdAt))
//      stmt.setTimestamp(9, java.sql.Timestamp.from(row.updatedAt))
//
//      stmt.executeUpdate()
//    }

  override def findById(id: ProjectId): AppTask[Option[Project]] = runQuery:
    run(quote {
      projectSchema.filter(_.id == lift(id.value))
    }).map(_.headOption.map(toDomain))

  override def findByUserId(userId: UserId): AppTask[List[Project]] = runQuery:
    run(quote {
      projectSchema
        .filter(_.ownerId == lift(userId.value))
        .sortBy(_.updatedAt)(using Ord.desc)
    }).map(_.map(toDomain))

  override def update(project: Project): AppTask[Unit] = transaction:
    val row = toRow(project)
    run(quote {
      projectSchema
        .filter(_.id == lift(project.id.value))
        .updateValue(lift(row))
    }).unit

  override def delete(id: ProjectId): AppTask[Unit] = transaction:
    run(quote {
      projectSchema.filter(_.id == lift(id.value)).delete
    }).unit

  private def toRow(domain: Project): ProjectRow =
    ProjectRow(
      id = domain.id.value,
      name = domain.name.value,
      description = domain.description,
      ownerId = domain.ownerId.value,
      settings = JsonbValue(domain.settings.toJsonAst),
      tags = domain.tags,
      isArchived = domain.isArchived,
      createdAt = domain.createdAt,
      updatedAt = domain.updatedAt
    )

  private def toDomain(entity: ProjectRow): Project =
    Project(
      id = ProjectId(entity.id),
      name = ProjectName.unsafe(entity.name),
      description = entity.description,
      ownerId = UserId(entity.ownerId),
      tags = entity.tags,
      settings = ProjectSettings.fromJsonAst(entity.settings.value),
      state = if entity.isArchived then ProjectState.Archived else ProjectState.Active,
      createdAt = entity.createdAt,
      updatedAt = entity.updatedAt
    )

object ProjectRepositoryLive:
  val live: ZLayer[DefaultDbContext & PostgresDataSource, Nothing, ProjectRepository] =
    ZLayer.fromFunction(ProjectRepositoryLive(_, _))
