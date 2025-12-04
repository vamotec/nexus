package app.mosia.nexus
package codegen

import io.github.cdimascio.dotenv.Dotenv
import slick.codegen.SourceCodeGenerator
import slick.jdbc.meta.MTable
import zio.Console.*
import zio.config.magnolia.deriveConfig
import zio.config.typesafe.TypesafeConfigProvider
import zio.logging.backend.SLF4J
import zio.logging.consoleLogger
import zio.{config, *}

import java.nio.file.Paths

object CodegenRunner extends ZIOAppDefault:
  // ============================================
  // Bootstrap & Configuration
  // ============================================
  override val bootstrap: ZLayer[ZIOAppArgs, Any, Unit] =
    Runtime.removeDefaultLoggers >>> consoleLogger() ++ SLF4J.slf4j ++
      ZLayer.fromZIO {
        ZIO
          .attempt {
            Dotenv
              .configure()
              .ignoreIfMissing()
              .systemProperties()
              .load()
          }
          .tapError(e => ZIO.logError(s"Failed to load .env file: ${e.getMessage}"))
          .orDie
      }.unit

  // ============================================
  // Banner
  // ============================================

  private def printBanner: UIO[Unit] =
    Console
      .printLine("""
        |╔═══════════════════════════════════════╗
        |║   Nexus Code Generation Tool        ║
        |╚═══════════════════════════════════════╝
    """.stripMargin)
      .orDie

  // ============================================
  // Code Generation Logic
  // ============================================

  private def processTableName(tableName: String): String = {
    val camelCase = tableName.split('_').map(_.capitalize).mkString("")
    if (camelCase.length > 2 && camelCase.endsWith("s") && !camelCase.endsWith("ss")) {
      camelCase.dropRight(1)
    } else {
      camelCase
    }
  }

  private def processColumnName(columnName: String): String = {
    if (columnName == "type") "`type`"
    else
      columnName
        .split('_')
        .zipWithIndex
        .map {
          case (part, 0) => part
          case (part, _) => part.capitalize
        }
        .mkString("")
  }

  private def mapColumnType(dbType: String, nullable: Boolean): String =
    val normalizedType = dbType match
      case "java.sql.Timestamp" => "java.time.Instant"
      case "java.sql.Blob" => "io.getquill.JsonbValue[zio.json.ast.Json]"
      case "java.sql.Clob" => "List[String]"
      case "java.sql.Array" => "List[Int]"
      case _ => dbType
    if (nullable) s"Option[$normalizedType]" else normalizedType

  private def generateCode(table: slick.model.Table, pkg: String): String = {
    val tableName          = table.name.table
    val processedTableName = processTableName(tableName)

    val fields = table.columns
      .map { column =>
        val processedColumnName = processColumnName(column.name)
        val columnType          = mapColumnType(column.tpe, column.nullable)
        s"  $processedColumnName: $columnType"
      }
      .mkString(",\n")

    s"""// AUTO-GENERATED QUILL MODEL TABLE. DO NOT EDIT.
       |package app.mosia.nexus
       |package $pkg
       |
       |case class ${processedTableName}Row(
       |$fields
       |)
       |""".stripMargin
  }

  // ============================================
  // Database Introspection & File Generation
  // ============================================

  private def generateForDatabaseZIO(
    jdbcUrl: String,
    user: String,
    password: String,
    outputDir: String,
    packageName: String
  ): Task[Unit] =
    ZIO.scoped {
      for
        db <- ZIO.acquireRelease(
          ZIO.succeed(
            slick.jdbc.PostgresProfile.api.Database.forURL(
              url = jdbcUrl,
              user = user,
              password = password,
              driver = "org.postgresql.Driver"
            )
          )
        )(db => ZIO.succeed(db.close()))

        _ <- ZIO
          .fromFuture { implicit ec =>
            for {
              mTables <- db.run(MTable.getTables(None, None, None, Some(Seq("TABLE"))))
              modelBuilder = new CustomPostgresModelBuilder(mTables, ignoreInvalidDefaults = true)
              model <- db.run(modelBuilder.buildModel) // 再次 db.run 解开 DBIO
            } yield model
          }
          .flatMap { model =>
            ZIO.foreach(model.tables) { table =>
              ZIO.attempt {
                val tableName          = table.name.table
                val processedTableName = {
                  val camelCase = tableName.split('_').map(_.capitalize).mkString("")
                  // 只在合理的情况下去除 s
                  if (camelCase.length > 2 && camelCase.endsWith("s") && !camelCase.endsWith("ss")) {
                    camelCase.dropRight(1)
                  } else {
                    camelCase
                  }
                }
                val newFileName  = s"${processedTableName}Row.scala"
                val tableContent = generateCode(table, packageName)
                val generator    = new SourceCodeGenerator(model)
                generator.writeStringToFile(tableContent, outputDir, packageName, newFileName)
              }
            }
          }
        _ <- printLine(s"  ✅Postgress models generated").orDie
      yield ()
    }

  private def generateForDatabase(
    jdbcUrl: String,
    user: String,
    password: String,
    outputDir: String,
    packageName: String
  ): Task[Int] =
    ZIO.scoped {
      for
        _ <- ZIO.logInfo(s"Connecting to database: $jdbcUrl")

        db <- ZIO.acquireRelease(
          ZIO.succeed(
            slick.jdbc.PostgresProfile.api.Database.forURL(
              url = jdbcUrl,
              user = user,
              password = password,
              driver = "org.postgresql.Driver"
            )
          )
        )(db => ZIO.succeed(db.close()))

        _ <- ZIO.logInfo("Fetching table metadata...")

        model <- ZIO
          .fromFuture { implicit ec =>
            for {
              mTables <- db.run(MTable.getTables(None, None, None, Some(Seq("TABLE"))))
              _            = println(s"Found ${mTables.length} tables")
              modelBuilder = new CustomPostgresModelBuilder(mTables, ignoreInvalidDefaults = true)
              model <- db.run(modelBuilder.buildModel)
            } yield model
          }
          .tapError(e => ZIO.logError(s"Failed to fetch database metadata: ${e.getMessage}"))

        tableCount = model.tables.length
        _ <- ZIO.logInfo(s"Found $tableCount tables to generate")

        _ <- ZIO.foreach(model.tables) { table =>
          ZIO
            .attempt {
              val tableName          = table.name.table
              val processedTableName = processTableName(tableName)
              val fileName           = s"${processedTableName}Row.scala"
              val content            = generateCode(table, packageName)
              val generator          = new SourceCodeGenerator(model)

              generator.writeStringToFile(content, outputDir, packageName, fileName)

              fileName
            }
            .tap(fileName => ZIO.logDebug(s"Generated: $fileName"))
        }

        _ <- ZIO.logInfo(s"✅ Successfully generated $tableCount model files")
      yield tableCount
    }

  // ============================================
  // Main Generation Method
  // ============================================

  private def generateQuillModels(config: DatabaseConfig): Task[Unit] =
    val projectRoot = Paths.get("").toAbsolutePath.toString
    val outputDir   = s"$projectRoot/modules/nexus/target/scala-3.7.4/src_managed/main"
    val outputPath  = Paths.get(outputDir)

    for
      _ <- ZIO.logInfo("Starting Quill model generation...")
      _ <- ZIO.logDebug(s"Output directory: $outputDir")
      _ <- ZIO.logDebug(s"Package: infrastructure.persistence.rows")

      count <- generateForDatabase(
        jdbcUrl = config.url,
        user = config.user,
        password = config.password,
        outputDir = outputDir,
        packageName = "infrastructure.persistence.rows"
      )

      _ <- ZIO.logInfo(s"✅ Code generation completed! Generated $count files.")
    yield ()

  // ============================================
  // Main Program
  // ============================================

  private val codegenProgram: ZIO[DatabaseConfig, Throwable, ExitCode] =
    for
      _ <- printBanner
      _ <- ZIO.logInfo("Initializing code generation...")

      config <- ZIO.service[DatabaseConfig]
      _ <- ZIO.logDebug(s"Database URL: ${config.url}")

      _ <- generateQuillModels(config)

      _ <- Console
        .printLine("""
          |╔═══════════════════════════════════════╗
          |║   Code Generation Completed ✓       ║
          |╚═══════════════════════════════════════╝
      """.stripMargin)
        .orDie
    yield ExitCode.success

  // ============================================
  // Application Entry Point
  // ============================================

  override def run: ZIO[Any, Any, ExitCode] =
    codegenProgram
      .catchAll { error =>
        ZIO.logError(s"❌ Code generation failed: ${error.getMessage}") *>
          ZIO.logErrorCause("Detailed error", Cause.fail(error)) *>
          Console.printLine(s"\n❌ Code generation failed: ${error.getMessage}").orDie *>
          ZIO.succeed(ExitCode.failure)
      }
      .provide(
        ZLayer.fromZIO(
          TypesafeConfigProvider
            .fromResourcePath()
            .nested("database")
            .load(deriveConfig[DatabaseConfig])
        )
      )
