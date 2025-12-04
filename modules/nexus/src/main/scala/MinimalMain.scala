package app.mosia.nexus

import zio.logging.backend.SLF4J
import zio.{Scope, URIO, ZIO, ZIOAppArgs, ZIOAppDefault, ZLayer, durationInt, Runtime as ZioRuntime}

object MinimalMain extends ZIOAppDefault:

  override val bootstrap: ZLayer[ZIOAppArgs, Any, Unit] =
    ZLayer.succeed(println("Bootstrap executed")) ++
      (ZioRuntime.removeDefaultLoggers ++ SLF4J.slf4j)

  override def run: ZIO[ZIOAppArgs & Scope, Any, Any] =
    for {
      _ <- ZIO.logInfo("✅ Minimal app started successfully!")
      _ <- ZIO.sleep(1.second)
      _ <- ZIO.logInfo("✅ Exiting...")
    } yield ()