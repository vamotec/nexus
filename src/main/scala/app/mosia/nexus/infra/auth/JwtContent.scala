package app.mosia.nexus.infra.auth

import caliban.CalibanError.ExecutionError
import zio.*

trait JwtContent:
  def get: UIO[Option[JwtPayload]]
  def set(payload: JwtPayload): UIO[Unit]

object JwtContent:
  val live: ULayer[JwtContent] = ZLayer.scoped:
    FiberRef
      .make[Option[JwtPayload]](None)
      .map: ref =>
        new JwtContent:
          override def get: UIO[Option[JwtPayload]] = ref.get

          override def set(payload: JwtPayload): UIO[Unit] = ref.set(Some(payload))

  def toExecutionError(e: Throwable): ExecutionError =
    ExecutionError(
      msg = e.getMessage,
      innerThrowable = Some(e)
    )
