package app.mosia.nexus
package infrastructure.persistence.postgres.repository

import infrastructure.persistence.BaseSource.PostgresDataSource
import infrastructure.persistence.postgres.rows.SessionRow
import domain.error.AppTask
import domain.model.project.ProjectId
import domain.model.resource.{ControlEndpoint, IsaacSimInstanceId, ResourceAssignment, StreamEndpoint}
import domain.model.session.{SessionId, SessionStatus, SimSession, SimulationConfigSnapshot}
import domain.model.simulation.SimulationId
import domain.model.user.UserId
import domain.repository.SessionRepository

import javax.sql.DataSource
import io.getquill.*
import io.getquill.context.json.*
import zio.{ZIO, ZLayer}

final class SessionRepositoryLive(ctx: DefaultDbContext, dataSource: PostgresDataSource)
    extends BaseRepository(ctx, dataSource)
    with SessionRepository:

  import ctx.*
  private inline def sessionSchema = querySchema[SessionRow]("sessions")

  override def delete(id: SessionId): AppTask[Unit] = transaction:
    val uuid = id.value
    run(sessionSchema.filter(_.id == lift(uuid)).delete).unit

  override def save(session: SimSession): AppTask[Unit] = transaction:
    val row = toRow(session)
    run(sessionSchema.insertValue(lift(row)).returningGenerated(_.id)).unit

  override def update(session: SimSession): AppTask[Unit] = transaction:
    val row = toRow(session)
    run(
      sessionSchema
        .filter(_.id == lift(row.id))
        .updateValue(lift(row))
    ).unit

  override def findById(id: SessionId): AppTask[Option[SimSession]] = runQuery:
    run(sessionSchema.filter(_.id == lift(id.value))).map(_.headOption.map(toDomain))

  override def findByUserId(userId: UserId, limit: Int): AppTask[List[SimSession]] = runQuery:
    run(
      sessionSchema
        .filter(_.userId == lift(userId.value))
        .sortBy(_.createdAt)(using Ord.desc)
        .take(lift(limit))
    ).map(_.map(toDomain))

  override def findBySimulationId(simulationId: SimulationId): AppTask[List[SimSession]] = runQuery:
    run(
      sessionSchema
        .filter(_.simulationId == lift(simulationId.value))
        .sortBy(_.createdAt)(using Ord.desc)
    ).map(_.map(toDomain))

  private def toRow(domain: SimSession): SessionRow =
    SessionRow(
      id = domain.id.value,
      userId = domain.userId.value,
      simulationId = domain.simulationId.value,
      projectId = domain.projectId.value,
      clusterId = domain.clusterId,
      mode = domain.mode.toString.toLowerCase,

      // 从配置快照中提取场景信息
      configSnapshot = JsonbValue(domain.configSnapshot.toJsonAst),
      configSnapshotVersion = domain.configSnapshot.version,
      // 状态
      status = domain.status.toString,

      // 资源分配信息
      isaacSimInstanceId = domain.resourceAssignment.map(_.isaacSimInstanceId.value),
      nucleusPath = domain.resourceAssignment.map(_.nucleusPath),
      streamHost = domain.resourceAssignment.map(_.streamEndpoint.host),
      streamPort = domain.resourceAssignment.map(_.streamEndpoint.port),
      streamProtocol = domain.resourceAssignment.map(_.streamEndpoint.protocol),
      controlWsUrl = domain.resourceAssignment.map(_.controlEndpoint.wsUrl),
      errorMessage = domain.error.map(_.message),
      errorCode = domain.error.map(_.code),
      // 时间戳
      createdAt = domain.createdAt,
      startedAt = domain.startedAt,
      completedAt = domain.completedAt
    )

  private def toDomain(row: SessionRow): SimSession =
    val resource: Option[ResourceAssignment] =
      for {
        isaacId <- row.isaacSimInstanceId.map(IsaacSimInstanceId(_))
        nucleus <- row.nucleusPath
        host <- row.streamHost
        port <- row.streamPort
        protocol <- row.streamProtocol
        controlUrl <- row.controlWsUrl
      } yield ResourceAssignment(
        isaacSimInstanceId = isaacId,
        nucleusPath = nucleus,
        streamEndpoint = StreamEndpoint(host, port, protocol),
        controlEndpoint = ControlEndpoint(controlUrl)
      )

    SimSession(
      id = SessionId(row.id),
      simulationId = SimulationId(row.simulationId),
      projectId = ProjectId(row.projectId),
      userId = UserId(row.userId),
      clusterId = row.clusterId,
      mode = domain.model.session.SessionMode.fromString(row.mode),
      configSnapshot = SimulationConfigSnapshot.fromJsonAst(row.configSnapshot.value),
      status = SessionStatus.fromString(row.status),
      resourceAssignment = resource,
      result = None,
      error = None,
      createdAt = row.createdAt,
      startedAt = row.startedAt,
      completedAt = row.completedAt
    )

object SessionRepositoryLive:
  val live: ZLayer[DefaultDbContext & PostgresDataSource, Nothing, SessionRepository] =
    ZLayer.fromFunction(SessionRepositoryLive(_, _))
