package app.mosia.nexus
package domain.services.infra

import domain.model.jwt.Payload

import zio.*
import zio.http.*
import zio.json.*

trait JwtContent:
  def get: UIO[Option[String]]
  def set(userId: String): UIO[Unit]

object JwtContent:
  val live: ULayer[JwtContent] = ZLayer.scoped:
    FiberRef
      .make[Option[String]](None)
      .map: ref =>
        new JwtContent:
          override def get: UIO[Option[String]] = ref.get

          override def set(userId: String): UIO[Unit] = ref.set(Some(userId))
