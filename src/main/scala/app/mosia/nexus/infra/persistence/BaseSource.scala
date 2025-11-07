package app.mosia.nexus.infra.persistence

import app.mosia.nexus.infra.config.AppConfig
import io.getquill.jdbczio.Quill
import zio.ZLayer

import javax.sql.DataSource

object BaseSource:

  val postgresLive: ZLayer[Any, Throwable, DataSource] =
    Quill.DataSource.fromPrefix("app.db.default")

  val timescaleLive: ZLayer[Any, Throwable, DataSource] =
    Quill.DataSource.fromPrefix("app.db.timescale")

  val combineDataSource: ZLayer[Any, Throwable, DataSource] = postgresLive ++ timescaleLive
