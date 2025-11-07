package app.mosia.nexus.infra.persistence.postgres.migration

import org.flywaydb.core.Flyway
import zio.{Task, ZIO}

class FlywayService(dataSource: javax.sql.DataSource):
  def migrate: Task[Unit] =
    ZIO.attempt {
      Flyway
        .configure()
        .dataSource(dataSource)
        .locations("classpath:db/migration")
        .load()
        .migrate()
    }.unit
