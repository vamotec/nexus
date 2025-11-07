package app.mosia.nexus.infra.persistence.postgres.repository

import app.mosia.nexus.infra.persistence.BaseSource
import io.getquill.*
import io.getquill.jdbczio.Quill
import zio.*

import javax.sql.DataSource

class DefaultDbContext extends PostgresZioJdbcContext(SnakeCase)

object DefaultDbContext:
  val live: ZLayer[Any, Throwable, DefaultDbContext] =
    BaseSource.postgresLive >>> ZLayer.succeed(DefaultDbContext())
