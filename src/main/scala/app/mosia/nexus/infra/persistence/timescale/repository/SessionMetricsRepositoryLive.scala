package app.mosia.nexus.infra.persistence.timescale.repository

import app.mosia.nexus.domain.model.common.Position3D
import app.mosia.nexus.domain.model.session.SessionMetrics
import app.mosia.nexus.domain.repository.SessionMetricsRepository
import app.mosia.nexus.infra.error.{AppTask, ZSQL}
import app.mosia.nexus.infra.persistence.JsonbSupport
import app.mosia.nexus.infra.persistence.timescale.entity.SessionMetricsEntity
import io.getquill.*
import io.getquill.jdbczio.Quill
import io.getquill.extras.InstantOps
import zio.{Task, ZIO}

import java.sql.SQLException
import java.time.Instant
import java.util.UUID
import javax.sql.DataSource

class SessionMetricsRepositoryLive(ctx: TimescaleDbContext, dataSource: DataSource)
    extends BaseRepository(ctx, dataSource)
    with SessionMetricsRepository
    with JsonbSupport:
  import ctx.*

  private inline def sessionMetricsSchema = querySchema[SessionMetricsEntity]("session_metrics")
  // 1. 插入
  override def insert(metric: SessionMetrics): AppTask[Unit] = runQuery:
    run(quote {
      sessionMetricsSchema.insertValue(lift(metric.toEntity)).returning(u => u)
    }).unit

  // 2. 查询最新快照
  override def findLatest(sessionId: UUID): AppTask[Option[SessionMetrics]] = runQuery:
    run(quote {
      sessionMetricsSchema
        .filter(_.sessionId.contains(lift(sessionId)))
        .sortBy(_.time)(using Ord.desc)
        .take(1)
    }).map(_.headOption.flatMap(entity => SessionMetrics.fromEntity(entity).toOption))

  // 3. 查询历史
  override def getHistory(
    sessionId: UUID,
    from: Instant,
    to: Instant,
    limit: Int
  ): AppTask[List[SessionMetrics]] = runQuery:
    run(quote {
      sessionMetricsSchema
        .filter(_.sessionId.contains(lift(sessionId)))
        .filter(m => m.time >= lift(from) && m.time <= lift(to))
        .sortBy(_.time)(using Ord.desc)
        .take(lift(limit))
    }).map(_.flatMap(entity => SessionMetrics.fromEntity(entity).toOption))
