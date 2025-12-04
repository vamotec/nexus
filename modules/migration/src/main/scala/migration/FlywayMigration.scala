package app.mosia.nexus
package migration

import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.MigrationInfo
import zio.*

import javax.sql.DataSource

enum DatabaseType:
  case Postgres, Timescale

case class MigrationResult(
  dbType: DatabaseType,
  migrationsExecuted: Int,
  targetVersion: String,
  success: Boolean
)

trait FlywayMigration:
  def migrate(dbType: DatabaseType): Task[MigrationResult]
  def validate(dbType: DatabaseType): Task[Unit]
  def info(dbType: DatabaseType): Task[List[MigrationInfo]]
  def baseline(dbType: DatabaseType): Task[Unit]

object FlywayMigration:

  private case class Live(config: MigrationConfig) extends FlywayMigration:

    private def createDataSource(dbConfig: DatabaseConfig): DataSource =
      val hikariConfig = new HikariConfig()
      hikariConfig.setJdbcUrl(dbConfig.url)
      hikariConfig.setUsername(dbConfig.user)
      hikariConfig.setPassword(dbConfig.password)
      hikariConfig.setMaximumPoolSize(dbConfig.maxPoolSize)
      hikariConfig.setConnectionTimeout(30000)
      new HikariDataSource(hikariConfig)

    private def getFlyway(dbType: DatabaseType): Flyway =
      val (dbConfig, location) = dbType match
        case DatabaseType.Postgres => (config.postgres, config.locations.postgres)
        case DatabaseType.Timescale => (config.postgres, "")

      val dataSource = createDataSource(dbConfig)

      Flyway
        .configure()
        .dataSource(dataSource)
        .locations(location)
        .baselineOnMigrate(config.baselineOnMigrate)
        .cleanDisabled(config.cleanDisabled)
        .load()

    override def migrate(dbType: DatabaseType): Task[MigrationResult] =
      ZIO.attemptBlocking {
        val flyway = getFlyway(dbType)
        val result = flyway.migrate()
        MigrationResult(
          dbType = dbType,
          migrationsExecuted = result.migrationsExecuted,
          targetVersion = result.targetSchemaVersion,
          success = result.success
        )
      }

    override def validate(dbType: DatabaseType): Task[Unit] =
      ZIO.attemptBlocking {
        val flyway = getFlyway(dbType)
        flyway.validate()
      }

    override def info(dbType: DatabaseType): Task[List[MigrationInfo]] =
      ZIO.attemptBlocking {
        val flyway = getFlyway(dbType)
        flyway.info().all().toList
      }

    override def baseline(dbType: DatabaseType): Task[Unit] =
      ZIO.attemptBlocking {
        val flyway = getFlyway(dbType)
        flyway.baseline()
      }.unit

  val layer: ZLayer[MigrationConfig, Nothing, FlywayMigration] =
    ZLayer.fromFunction(Live.apply)
