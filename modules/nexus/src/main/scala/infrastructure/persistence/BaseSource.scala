package app.mosia.nexus
package infrastructure.persistence

import javax.sql.DataSource
import io.getquill.jdbczio.Quill
import zio.*

object BaseSource:
  case class PostgresDataSource(ds: DataSource) extends AnyVal

  val postgresLive: ZLayer[Any, Throwable, PostgresDataSource] =
    ZLayer.scoped(
      Quill.DataSource
        .fromPrefix("app.db")
        .build
        .map(env => PostgresDataSource(env.get[DataSource]))
    )
