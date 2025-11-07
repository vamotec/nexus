package app.mosia.nexus.infra.persistence.redis

import zio.*
import zio.json.*
import zio.redis.*

final class RedisServiceLive(redis: Redis) extends RedisService:
  override def get[T: JsonDecoder](key: String): IO[RedisError, Option[T]] =
    redis.get(key).returning[String].map(_.flatMap(_.fromJson[T].toOption))

  override def set[T: JsonEncoder](key: String, value: T, expiration: Option[Duration] = None): IO[RedisError, Unit] =
    redis.set(key, value.toJson, expiration).unit

object RedisServiceLive:
  // 我们服务的 ZIO Layer 实现
  val live: ZLayer[Redis, Nothing, RedisService] =
    ZLayer.fromFunction(new RedisServiceLive(_))

  /** 单节点 Redis */
  val singleNode: ZLayer[Any, Throwable, Redis] =
    (
      ZLayer.succeed[CodecSupplier](ProtobufCodecSupplier) ++
        ZLayer.succeed(RedisConfig.Local)
    ) >>> Redis.singleNode

  /** Redis Cluster 模式 */
  val cluster: ZLayer[Any, Throwable, Redis] =
    val clusterConfig = RedisClusterConfig(
      addresses = Chunk(
        RedisUri("localhost", 7000),
        RedisUri("localhost", 7001),
        RedisUri("localhost", 7002)
      ),
      retry = RetryClusterConfig(base = 100.millis, factor = 1.5, maxRecurs = 5)
    )

    (
      ZLayer.succeed[CodecSupplier](ProtobufCodecSupplier) ++
        ZLayer.succeed(clusterConfig)
    ) >>> Redis.cluster
