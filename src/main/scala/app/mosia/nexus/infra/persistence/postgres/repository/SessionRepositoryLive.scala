package app.mosia.nexus.infra.persistence.postgres.repository

import app.mosia.nexus.domain.model.session.{NeuroSession, SessionId}
import app.mosia.nexus.domain.model.simulation.SimulationId
import app.mosia.nexus.domain.model.user.UserId
import app.mosia.nexus.domain.repository.SessionRepository
import app.mosia.nexus.infra.error.*
import zio.{Task, ULayer, ZLayer}

import javax.sql.DataSource

final class SessionRepositoryLive(ctx: DefaultDbContext, dataSource: DataSource)
    extends BaseRepository(ctx, dataSource)
    with SessionRepository:
  import ctx.*

  override def delete(id: SessionId): AppTask[Unit] = ???

  override def save(session: NeuroSession): AppTask[Unit] = ???

  override def update(session: NeuroSession): AppTask[Unit] = ???

  override def findById(id: SessionId): AppTask[Option[NeuroSession]] = ???

  override def findByUserId(userId: UserId, limit: Int): AppTask[List[NeuroSession]] = ???

  override def findBySimulationId(simulationId: SimulationId): AppTask[List[NeuroSession]] = ???

object SessionRepositoryLive:
  val live: ZLayer[DefaultDbContext & DataSource, Nothing, SessionRepository] =
    ZLayer.fromFunction(SessionRepositoryLive(_, _))
