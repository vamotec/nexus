package app.mosia.nexus
package infrastructure.persistence

import javax.sql.DataSource
import io.getquill.jdbczio.Quill
import zio.*

object BaseSource:
  case class PostgresDataSource(ds: DataSource) extends AnyVal

  case class TimescaleDataSource(ds: DataSource) extends AnyVal

  val postgresLive: ZLayer[Any, Throwable, PostgresDataSource] =
    ZLayer.scoped(
      Quill.DataSource
        .fromPrefix("app.db.default")
        .build
        .map(env => PostgresDataSource(env.get[DataSource]))
    )

  val timescaleLive: ZLayer[Any, Throwable, TimescaleDataSource] =
    ZLayer.scoped(
      Quill.DataSource
        .fromPrefix("app.db.timescale")
        .build
        .map(env => TimescaleDataSource(env.get[DataSource]))
    )

  val allDataSources: ZLayer[Any, Throwable, PostgresDataSource & TimescaleDataSource] =
    postgresLive ++ timescaleLive
