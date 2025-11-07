package app.mosia.nexus.infra.persistence.timescale.repository

import app.mosia.nexus.infra.persistence.BaseSource
import io.getquill.{PostgresZioJdbcContext, SnakeCase}
import io.getquill.jdbczio.Quill
import zio.ZLayer

import javax.sql.DataSource

case class TimescaleDbContext() extends PostgresZioJdbcContext(SnakeCase)

object TimescaleDbContext:
  val live: ZLayer[Any, Throwable, TimescaleDbContext] =
    BaseSource.timescaleLive >>> ZLayer.succeed(TimescaleDbContext())
