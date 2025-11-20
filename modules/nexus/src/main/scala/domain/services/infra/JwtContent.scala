package app.mosia.nexus
package domain.services.infra

import domain.model.jwt.JwtPayload

import zio.*
import zio.http.*
import zio.json.*

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
