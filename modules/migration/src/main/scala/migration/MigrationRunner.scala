package app.mosia.nexus
package migration

import io.github.cdimascio.dotenv.Dotenv
import zio.Console.*
import zio.config.magnolia.deriveConfig
import zio.config.typesafe.TypesafeConfigProvider
import zio.logging.backend.SLF4J
import zio.{config, *}

object MigrationRunner extends ZIOAppDefault:
  override val bootstrap: URLayer[ZIOAppArgs, Dotenv] =
    ZLayer.fromZIO {
      ZIO.attempt {
        Dotenv
          .configure()
          .ignoreIfMissing()
          .systemProperties() // 自动设置到 java.lang.System properties
          .load()
      }.orDie
    } ++ Runtime.removeDefaultLoggers ++ SLF4J.slf4j

  private def printBanner: UIO[Unit] =
    printLine("""
                |╔═══════════════════════════════════════╗
                |║   Mosia Database Migration Tool     ║
                |╚═══════════════════════════════════════╝
    """.stripMargin).orDie

  private def printMigrationInfo(infos: List[org.flywaydb.core.api.MigrationInfo]): UIO[Unit] =
    ZIO.foreachDiscard(infos) { info =>
      printLine(
        f"  ${info.getVersion}%-10s ${info.getDescription}%-40s ${info.getState}"
      ).orDie
    }

  private def showPendingMigrations(
    service: FlywayMigration,
    dbType: DatabaseType
  ): Task[List[org.flywaydb.core.api.MigrationInfo]] =
    for
      _ <- printLine(s"\n📋 Checking $dbType migrations...").orDie
      allInfo <- service.info(dbType)
      pending = allInfo.filter(_.getState.name() == "PENDING")
      _ <-
        if pending.isEmpty then printLine(s"  ✅ No pending migrations for $dbType").orDie
        else
          printLine(s"  ⚠️  Found ${pending.length} pending migrations:").orDie *>
            printMigrationInfo(pending)
    yield pending

  private def confirmMigration(totalPending: Int): Task[Boolean] =
    if totalPending == 0 then ZIO.succeed(true)
    else
      for
        _ <- printLine(s"\n⚠️  About to apply $totalPending migrations").orDie
        _ <- printLine("Do you want to proceed? (yes/no): ").orDie
        input <- readLine.orDie
      yield input.toLowerCase == "yes"

  private def executeMigration(
    service: FlywayMigration,
    dbType: DatabaseType
  ): Task[MigrationResult] =
    for
      _ <- printLine(s"\n🚀 Migrating $dbType...").orDie
      result <- service.migrate(dbType)
      _ <-
        if result.success then
          printLine(s"  ✅ $dbType: Applied ${result.migrationsExecuted} migrations → ${result.targetVersion}").orDie
        else printLine(s"  ❌ $dbType: Migration failed!").orDie
    yield result

  private def validateMigrations(service: FlywayMigration): Task[Unit] =
    for
      _ <- printLine("\n🔍 Validating migrations...").orDie
      _ <- service.validate(DatabaseType.Postgres)
      _ <- printLine("  ✅ All migrations validated successfully").orDie
    yield ()

  private val migrationProgram: ZIO[FlywayMigration & MigrationConfig, Throwable, ExitCode] =
    for
      config <- ZIO.service[MigrationConfig]
      service <- ZIO.service[FlywayMigration]
      _ <- printBanner
      // 如果只是验证模式
      _ <- ZIO.when(config.validateOnly) {
        validateMigrations(service) *>
          printLine("\n✅ Validation completed. No migrations applied.").orDie *>
          ZIO.succeed(ExitCode.success)
      }
      // 显示待执行的迁移
      postgresPending <- showPendingMigrations(service, DatabaseType.Postgres)
      totalPending = postgresPending.length
      // 确认执行
      confirmed <- confirmMigration(totalPending)
      exitCode <-
        if !confirmed then printLine("\n❌ Migration cancelled by user").orDie.as(ExitCode.failure)
        else if totalPending == 0 then
          printLine("\n✅ Database is up to date. No migrations needed.").orDie.as(ExitCode.success)
        else
          // 执行迁移
          for
            pgResult <- executeMigration(service, DatabaseType.Postgres)
            _ <- validateMigrations(service)
            _ <- printLine(s"""
                            |
                            |╔═══════════════════════════════════════╗
                            |║      Migration Completed              ║
                            |╠═══════════════════════════════════════╣
                            |║  PostgreSQL: ${pgResult.migrationsExecuted} migrations         ║
                            |║                                       ║
                            |╚═══════════════════════════════════════╝
                    """.stripMargin).orDie
          yield ExitCode.success
    yield exitCode

  override def run: ZIO[Any, Any, ExitCode] =
    migrationProgram
      .catchAll { error =>
        printLine(s"\n❌ Migration failed: ${error.getMessage}").orDie *>
          ZIO.logErrorCause("Migration error", Cause.fail(error)) *>
          ZIO.succeed(ExitCode.failure)
      }
      .provide(
        FlywayMigration.layer,
        ZLayer.fromZIO(
          TypesafeConfigProvider
            .fromResourcePath()
            .nested("migration")
            .load(deriveConfig[MigrationConfig])
        )
      )
