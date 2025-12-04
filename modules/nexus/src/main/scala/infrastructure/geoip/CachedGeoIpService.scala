package app.mosia.nexus
package infrastructure.geoip

import domain.config.AppConfig
import domain.config.cloud.GeoLocation
import domain.error.*
import domain.services.infra.{GeoIpService, RedisService}

import zio.*
import zio.json.*
import zio.http.*

final class CachedGeoIpService(
  underlying: GeoIpService,
  redisService: RedisService,
  cacheTtl: Duration = 1.hour
) extends GeoIpService:
  private def cacheKey(ip: String): String = s"geoip:$ip"

  override def lookup(ipAddress: String): ZIO[Scope, AppError, GeoLocation] =
    // 本地 IP 不缓存，直接返回
    if isLocalIp(ipAddress) then ZIO.succeed(GeoLocation(0.0, 0.0, Some("LOCAL"), Some("localhost")))
    else
      for {
        // 1. 尝试从 Redis 读取
        cached <- redisService
          .get(cacheKey(ipAddress))
          .mapError(err => GeneralServiceError("redis", "luckup", Some(err)))

        location <- cached match {
          case Some(json) =>
            // 缓存命中
            ZIO
              .fromEither(json.toJson.fromJson[GeoLocation])
              .mapError(err =>
                InvalidInput(
                  field = "json",
                  reason = s"Failed to parse cached: $err"
                )
              )

          case None =>
            // 缓存未命中，查询并缓存
            for {
              location <- underlying.lookup(ipAddress)

              // 写入 Redis
              json = location.toJson
              _ <- redisService.set(cacheKey(ipAddress), json, cacheTtl.toSeconds).orDie

            } yield location
        }

      } yield location

  private def isLocalIp(ip: String): Boolean =
    ip == "127.0.0.1" ||
      ip == "localhost" ||
      ip.startsWith("192.168.") ||
      ip.startsWith("10.") ||
      ip.startsWith("172.")

object CachedGeoIpService:
  def make(
    underlying: GeoIpService,
    redis: RedisService,
    cacheTtl: Duration
  ): GeoIpService = CachedGeoIpService(underlying, redis, cacheTtl)

  val geoIpLayer: ZLayer[Client & RedisService & AppConfig, Nothing, GeoIpService] =
    ZLayer.fromZIO {
      for {
        client <- ZIO.service[Client]
        redis <- ZIO.service[RedisService]
        config <- ZIO.service[AppConfig]

        // 先创建基础服务
        baseService = IpApiGeoIpService.make(client)

        // 如果启用缓存，包装一层
        service =
          if config.cache.cacheEnabled then CachedGeoIpService.make(baseService, redis, config.cache.geoip.cacheTtl)
          else baseService

      } yield service
    }
