package app.mosia.nexus.infra.persistence.redis

import zio.*
import zio.json.*
import zio.redis.*

trait RedisService:
  def get[T: JsonDecoder](key: String): IO[RedisError, Option[T]]
  def set[T: JsonEncoder](key: String, value: T, expiration: Option[Duration] = None): IO[RedisError, Unit]
