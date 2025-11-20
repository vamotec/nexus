package app.mosia.nexus
package domain.services.infra

import zio.json.*
import zio.*
import zio.redis.RedisError

trait RedisService:
  def get[T: JsonDecoder](key: String): IO[RedisError, Option[T]]
  def set[T: JsonEncoder](key: String, value: T, expiration: Option[Duration] = None): IO[RedisError, Unit]
