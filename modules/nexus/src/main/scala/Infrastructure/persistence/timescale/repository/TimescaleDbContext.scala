package app.mosia.nexus
package infrastructure.persistence.timescale.repository

import infrastructure.persistence.BaseSource

import javax.sql.DataSource
import io.getquill.jdbczio.Quill
import io.getquill.{PostgresZioJdbcContext, SnakeCase}
import zio.*

case class TimescaleDbContext() extends PostgresZioJdbcContext(SnakeCase)

object TimescaleDbContext:
  val live: ZLayer[Any, Throwable, TimescaleDbContext] =
    BaseSource.timescaleLive >>> ZLayer.succeed(TimescaleDbContext())
