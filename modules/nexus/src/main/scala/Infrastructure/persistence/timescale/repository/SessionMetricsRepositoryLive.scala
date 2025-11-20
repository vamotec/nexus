package app.mosia.nexus
package infrastructure.persistence.timescale.repository

import infrastructure.persistence.BaseSource.TimescaleDataSource
import infrastructure.persistence.timescale.rows.*
import domain.error.AppTask
import domain.model.common.Position3D
import domain.model.metrics.{AggregatedMetrics, AggregationInterval, SimSessionMetrics}
import domain.model.session.SessionId
import domain.model.simulation.SimulationId
import domain.repository.SessionMetricsRepository

import zio.*

import java.time.Instant
import java.util.UUID
import javax.sql.DataSource
import io.getquill.*
import io.getquill.context.json.*
import io.getquill.extras.InstantOps

class SessionMetricsRepositoryLive(ctx: TimescaleDbContext, dataSource: TimescaleDataSource)
    extends BaseRepository(ctx, dataSource)
    with SessionMetricsRepository:

  import ctx.*
  
  private inline def snapshotSchema = querySchema[SessionMetricsSnapshotRow]("session_metrics_snapshot")
  private inline def historySchema  = querySchema[SessionMetricsHistoryRow]("session_metrics_history")
  private inline def hourSchema     = querySchema[SessionMetrics1HourRow]("session_metrics_1hour")
  private inline def minSchema      = querySchema[SessionMetrics1MinRow]("session_metrics_1min")

  override def updateSnapshot(metrics: SimSessionMetrics): AppTask[Unit] = transaction:
    run(
      quote {
        snapshotSchema
          .insertValue(lift(toSnapshot(metrics)))
          .onConflictUpdate(_.sessionId)(
            (t, e) => t.currentFps -> e.currentFps,
            (t, e) => t.frameCount -> e.frameCount,
            (t, e) => t.simulationTime -> e.simulationTime,
            (t, e) => t.wallTime -> e.wallTime,
            (t, e) => t.robotPositionX -> e.robotPositionX,
            (t, e) => t.robotPositionY -> e.robotPositionY,
            (t, e) => t.robotPositionZ -> e.robotPositionZ,
            (t, e) => t.gpuUtilization -> e.gpuUtilization,
            (t, e) => t.gpuMemoryMb -> e.gpuMemoryMb,
            (t, e) => t.tags -> e.tags,
            (t, e) => t.updatedAt -> lift(Instant.now())
          )
      }
    ).unit

  // 记录历史 (INSERT)
  override def recordHistory(metrics: SimSessionMetrics): AppTask[Unit] = transaction:
    run(
      quote {
        historySchema
          .insertValue(lift(toHistory(metrics)))
      }
    ).unit

  // 获取最新快照
  override def getLatest(sessionId: UUID): AppTask[Option[SimSessionMetrics]] = runQuery:
    run(
      quote {
        snapshotSchema
          .filter(_.sessionId == lift(sessionId))
      }
    ).map(_.headOption.map(fromSnapshot))

  // 获取历史数据
  override def getHistory(
    sessionId: UUID,
    from: Instant,
    to: Instant
  ): AppTask[List[SimSessionMetrics]] = runQuery:
    run(
      quote {
        historySchema
          .filter(m =>
            m.sessionId == lift(sessionId) &&
              m.time >= lift(from) &&
              m.time <= lift(to)
          )
          .sortBy(_.time)(using Ord.desc)
      }
    ).map(_.map(fromHistory))

  override def getAggregated(
    sessionId: UUID,
    from: Instant,
    to: Instant,
    interval: AggregationInterval
  ): AppTask[List[AggregatedMetrics]] = runQuery:
    interval match
      case AggregationInterval.Hour | AggregationInterval.Day =>
        // 使用小时级聚合
        run(
          quote {
            hourSchema
              .filter(m =>
                m.sessionId == lift(sessionId) &&
                  m.bucket >= lift(from) &&
                  m.bucket <= lift(to)
              )
              .sortBy(_.bucket)(using Ord.desc)
          }
        ).map(_.map(from1Hour))

      case AggregationInterval.Minute =>
        // 使用分钟级聚合
        run(
          quote {
            minSchema
              .filter(m =>
                m.sessionId == lift(sessionId) &&
                  m.bucket >= lift(from) &&
                  m.bucket <= lift(to)
              )
              .sortBy(_.bucket)(using Ord.desc)
          }
        ).map(_.map(from1Min))

  override def getMultiSessionAggregated(
    simulationId: UUID,
    from: Instant,
    to: Instant,
    interval: AggregationInterval
  ): AppTask[List[AggregatedMetrics]] = runQuery:
    interval match
      case AggregationInterval.Hour | AggregationInterval.Day =>
        run(
          quote {
            hourSchema
              .filter(m =>
                m.simulationId == lift(simulationId) &&
                  m.bucket >= lift(from) &&
                  m.bucket <= lift(to)
              )
              .sortBy(_.bucket)(using Ord.desc)
          }
        ).map(_.map(from1Hour))

      case AggregationInterval.Minute =>
        run(
          quote {
            minSchema
              .filter(m =>
                m.simulationId == lift(simulationId) &&
                  m.bucket >= lift(from) &&
                  m.bucket <= lift(to)
              )
              .sortBy(_.bucket)(using Ord.desc)
          }
        ).map(_.map(from1Min))

  private def toSnapshot(domain: SimSessionMetrics): SessionMetricsSnapshotRow =
    SessionMetricsSnapshotRow(
      updatedAt = Instant.now(),
      simulationId = domain.simulationId.value,
      sessionId = domain.sessionId.value,
      currentFps = domain.currentFps,
      frameCount = domain.frameCount,
      simulationTime = domain.simulationTime,
      wallTime = domain.wallTime,
      robotPositionX = domain.robotPosition.x,
      robotPositionY = domain.robotPosition.y,
      robotPositionZ = domain.robotPosition.z,
      gpuUtilization = domain.gpuUtilization,
      gpuMemoryMb = domain.gpuMemoryMB,
      tags = domain.tags.map(v => JsonbValue(v))
    )

  private def toHistory(domain: SimSessionMetrics): SessionMetricsHistoryRow =
    SessionMetricsHistoryRow(
      time = Instant.now(),
      simulationId = domain.simulationId.value,
      sessionId = domain.sessionId.value,
      currentFps = domain.currentFps,
      frameCount = domain.frameCount,
      simulationTime = domain.simulationTime,
      wallTime = domain.wallTime,
      robotPositionX = domain.robotPosition.x,
      robotPositionY = domain.robotPosition.y,
      robotPositionZ = domain.robotPosition.z,
      gpuUtilization = domain.gpuUtilization,
      gpuMemoryMb = domain.gpuMemoryMB,
      tags = domain.tags.map(v => JsonbValue(v))
    )

  private def from1Min(m: SessionMetrics1MinRow): AggregatedMetrics =
    AggregatedMetrics(
      bucket = m.bucket,
      sessionId = SessionId(m.sessionId),
      simulationId = SimulationId(m.simulationId),
      avgFps = m.avgFps,
      maxFps = m.maxFps,
      minFps = m.minFps,
      avgGpuUtilization = m.avgGpuUtil,
      maxGpuMemoryMb = m.maxGpuMemory
    )

  private def from1Hour(m: SessionMetrics1HourRow): AggregatedMetrics =
    AggregatedMetrics(
      bucket = m.bucket,
      sessionId = SessionId(m.sessionId),
      simulationId = SimulationId(m.simulationId),
      avgFps = m.avgFps,
      maxFps = 0.0, // 小时级别不保留 max/min
      minFps = 0.0,
      p50Fps = Some(m.p50Fps),
      p99Fps = Some(m.p99Fps),
      avgGpuUtilization = m.avgGpuUtil,
      maxGpuMemoryMb = 0L
    )

  private def fromSnapshot(row: SessionMetricsSnapshotRow): SimSessionMetrics =
    SimSessionMetrics(
      sessionId = SessionId(row.sessionId),
      simulationId = SimulationId(row.simulationId),
      currentFps = row.currentFps,
      frameCount = row.frameCount,
      simulationTime = row.simulationTime,
      wallTime = row.wallTime,
      robotPosition = Position3D(
        row.robotPositionX,
        row.robotPositionY,
        row.robotPositionZ
      ),
      gpuUtilization = row.gpuUtilization,
      gpuMemoryMB = row.gpuMemoryMb,
      updatedAt = row.updatedAt,
      tags = row.tags.map(_.value)
    )

  private def fromHistory(row: SessionMetricsHistoryRow): SimSessionMetrics =
    SimSessionMetrics(
      sessionId = SessionId(row.sessionId),
      simulationId = SimulationId(row.simulationId),
      currentFps = row.currentFps,
      frameCount = row.frameCount,
      simulationTime = row.simulationTime,
      wallTime = row.wallTime,
      robotPosition = Position3D(
        row.robotPositionX,
        row.robotPositionY,
        row.robotPositionZ
      ),
      gpuUtilization = row.gpuUtilization,
      gpuMemoryMB = row.gpuMemoryMb,
      updatedAt = row.time,
      tags = row.tags.map(_.value)
    )

object SessionMetricsRepositoryLive:
  val live: ZLayer[TimescaleDbContext & TimescaleDataSource, Nothing, SessionMetricsRepository] =
    ZLayer.fromFunction(SessionMetricsRepositoryLive(_, _))
