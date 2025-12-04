package app.mosia.nexus
package infrastructure.persistence

import infrastructure.persistence.BaseSource

import io.getquill.*
import zio.ZLayer

class DefaultDbContext extends PostgresZioJdbcContext(SnakeCase)

object DefaultDbContext:
  val live: ZLayer[Any, Throwable, DefaultDbContext] =
    BaseSource.postgresLive >>> ZLayer.succeed(DefaultDbContext())
