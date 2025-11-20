package app.mosia.nexus
package infrastructure.persistence.postgres.repository

import infrastructure.persistence.BaseSource.PostgresDataSource
import infrastructure.persistence.postgres.rows.AuditLogRow
import domain.error.AppTask
import domain.model.audit.{AuditAction, AuditLog, AuditLogFilters, AuditLogId}
import domain.model.user.UserId
import domain.repository.AuditLogRepository

import zio.*

import java.time.Instant
import io.getquill.*
import io.getquill.extras.InstantOps

class AuditLogRepositoryLive(
  ctx: DefaultDbContext,
  dataSource: PostgresDataSource
) extends BaseRepository(ctx, dataSource)
    with AuditLogRepository:
  private inline def auditLogs = querySchema[AuditLogRow]("audit_logs")

  import ctx.*
  override def insert(log: AuditLog): AppTask[AuditLog] =
    val entity = toRow(log)
    transaction {
      run(quote {
        auditLogs.insertValue(lift(entity)).returning(l => l)
      })
    }.map(toDomain)

  override def findById(id: AuditLogId): AppTask[Option[AuditLog]] =
    runQuery {
      run(quote {
        auditLogs.filter(_.id == lift(id.value))
      })
    }.map(_.headOption.map(toDomain))

  override def findByUserId(userId: UserId, limit: Int): AppTask[List[AuditLog]] =
    runQuery {
      run(quote {
        auditLogs
          .filter(_.userId.contains(lift(userId.value)))
          .sortBy(_.createdAt)(using Ord.desc)
          .take(lift(limit))
      })
    }.map(_.map(toDomain))

  override def findByFilters(filters: AuditLogFilters, limit: Int): AppTask[List[AuditLog]] =
    runQuery {
      val query = buildFilterQuery(filters, limit)
      run(query)
    }.map(_.map(toDomain))

  private def buildFilterQuery(filters: AuditLogFilters, limit: Int): Quoted[Query[AuditLogRow]] =
    val base = quote {
      auditLogs
    }

    val filtered = applyFilters(base, filters)

    quote {
      filtered
        .sortBy(_.createdAt)(using Ord.desc)
        .take(lift(limit))
    }

  private def applyFilters(
    query: EntityQuery[AuditLogRow],
    filters: AuditLogFilters
  ): Quoted[EntityQuery[AuditLogRow]] =
    var q = query

    // userId 过滤
    filters.userId.foreach { uid =>
      q = quote {
        q.filter(log => log.userId.contains(lift(uid.value)))
      }
    }

    // actions 过滤
    filters.actions.foreach { actions =>
      val actionStrings = actions.map(_.toString)
      q = quote {
        q.filter(log => liftQuery(actionStrings).contains(log.action))
      }
    }

    // resourceType 过滤
    filters.resourceType.foreach { rt =>
      q = quote {
        q.filter(log => log.resourceType.contains(lift(rt)))
      }
    }

    // resourceId 过滤
    filters.resourceId.foreach { rid =>
      q = quote {
        q.filter(log => log.resourceId.contains(lift(rid)))
      }
    }

    // 日期范围过滤
    filters.startDate.foreach { start =>
      q = quote {
        q.filter(log => log.createdAt >= lift(start))
      }
    }

    filters.endDate.foreach { end =>
      q = quote {
        q.filter(log => log.createdAt <= lift(end))
      }
    }

    // success 过滤
    filters.successOnly.foreach { successOnly =>
      q = quote {
        q.filter(log => log.success == lift(successOnly))
      }
    }

    // platform 过滤
    filters.platform.foreach { p =>
      q = quote {
        q.filter(log => log.platform.contains(lift(p)))
      }
    }

    q

  override def findRecent(limit: Int): AppTask[List[AuditLog]] =
    runQuery {
      run(quote {
        auditLogs
          .sortBy(_.createdAt)(using Ord.desc)
          .take(lift(limit))
      })
    }.map(_.map(toDomain))

  override def countByUserId(userId: UserId): AppTask[Long] =
    runQuery {
      run(quote {
        auditLogs
          .filter(_.userId.contains(lift(userId.value)))
          .size
      })
    }

  override def deleteOlderThan(timestamp: Instant): AppTask[Long] =
    runQuery {
      run(quote {
        auditLogs
          .filter(_.createdAt < lift(timestamp))
          .delete
      })
    }

  // ============ 转换方法 ============
  private def toDomain(entity: AuditLogRow): AuditLog =
    AuditLog(
      id = AuditLogId(entity.id),
      userId = entity.userId.map(UserId(_)),
      action = AuditAction
        .fromString(entity.action)
        .getOrElse(AuditAction.UserCreated), // 默认值
      resourceType = entity.resourceType,
      resourceId = entity.resourceId,
      details = entity.details.map(_.value),
      ipAddress = entity.ipAddress.map(_.value),
      userAgent = entity.userAgent,
      platform = entity.platform,
      success = entity.success,
      errorMessage = entity.errorMessage,
      createdAt = entity.createdAt
    )

  private def toRow(log: AuditLog): AuditLogRow =
    AuditLogRow(
      id = log.id.value,
      userId = log.userId.map(_.value),
      action = log.action.toString,
      resourceType = log.resourceType,
      resourceId = log.resourceId,
      details = log.details.map(v => JsonbValue(v)),
      ipAddress = log.ipAddress.map(v => JsonbValue(v)),
      userAgent = log.userAgent,
      platform = log.platform,
      success = log.success,
      errorMessage = log.errorMessage,
      createdAt = log.createdAt
    )

object AuditLogRepositoryLive:
  val live: ZLayer[DefaultDbContext & PostgresDataSource, Nothing, AuditLogRepository] =
    ZLayer.fromFunction(AuditLogRepositoryLive(_, _))
