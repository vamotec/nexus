package app.mosia.nexus

import domain.config.AppConfig

import io.github.cdimascio.dotenv.Dotenv
import zio.config.magnolia.deriveConfig
import zio.config.typesafe.TypesafeConfigProvider
import zio.logging.backend.SLF4J
import zio.{Scope, ZIO, ZIOAppArgs, ZIOAppDefault, ZLayer, Runtime as ZioRuntime}

object MainDebug extends ZIOAppDefault:

  override val bootstrap: ZLayer[ZIOAppArgs, Any, Unit] =
    ZLayer.fromZIO {
      for {
        _ <- ZIO.succeed(println("=== BOOTSTRAP START ==="))

        // 加载 .env
        dotenvResult <- ZIO.attempt {
          println("Loading .env...")
          val dotenv = Dotenv
            .configure()
            .ignoreIfMissing()
            .systemProperties()
            .load()
          println(s".env loaded: ${dotenv.entries().size()} entries")
          dotenv
        }.either

        _ <- dotenvResult match {
          case Right(_) => ZIO.succeed(println("✅ .env loaded successfully"))
          case Left(e) => ZIO.succeed {
            println(s"⚠️  .env load failed: ${e.getMessage}")
            e.printStackTrace()
          }
        }

        _ <- ZIO.succeed(println("=== BOOTSTRAP END ===\n"))
      } yield ()
    } ++ (ZioRuntime.removeDefaultLoggers ++ SLF4J.slf4j)

  override def run: ZIO[ZIOAppArgs & Scope, Any, Any] =
    (for {
      _ <- ZIO.succeed(println("\n=== RUN START ==="))

      // 测试配置加载
      _ <- ZIO.succeed(println("Loading configuration..."))
      configResult <- ZIO.service[AppConfig]
        .provide(
          ZLayer.fromZIO(
            TypesafeConfigProvider
              .fromResourcePath()
              .nested("app")
              .load(deriveConfig[AppConfig])
          )
        )

      _ <- ZIO.succeed {
        println("✅ Configuration loaded successfully")
        println(s"HTTP: ${configResult.http.host}:${configResult.http.port}")
      }

      _ <- ZIO.succeed(println("=== RUN END ===\n"))

    } yield ())
      .catchAllCause { cause =>
        ZIO.succeed {
          println("\n❌ FATAL ERROR:")
          println(cause.prettyPrint)
        }
      }
