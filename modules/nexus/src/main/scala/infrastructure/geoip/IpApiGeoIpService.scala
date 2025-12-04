package app.mosia.nexus
package infrastructure.geoip

import application.dto.response.IpApiResponse
import domain.config.cloud.GeoLocation
import domain.error.*
import domain.services.infra.GeoIpService

import zio.*
import zio.json.*
import zio.http.*

final class IpApiGeoIpService(client: Client) extends GeoIpService:

  override def lookup(ipAddress: String): ZIO[Scope, AppError, GeoLocation] =
    // 本地 IP 直接返回
    if isLocalIp(ipAddress) then ZIO.succeed(GeoLocation(0.0, 0.0, Some("LOCAL"), Some("localhost")))
    else
      for {
        response <- client
          .request(
            Request.get(URL.decode(s"http://ip-api.com/json/$ipAddress").toOption.get)
          )
          .mapError(err => GeneralServiceError("ip-api", "client request", Some(err)))

        body <- response.body.asString
          .mapError(err => InvalidInput("geoip", s"Failed to read response: ${err.getMessage}"))

        apiResponse <- ZIO
          .fromEither(body.fromJson[IpApiResponse])
          .mapError(err => InvalidInput("json", s"Failed to parse JSON: $err"))

        _ <- ZIO.when(apiResponse.status != "success")(
          ZIO.fail(InvalidInput("ip-api", s"IP lookup failed for [$ipAddress]"))
        )

        location = GeoLocation(
          latitude = apiResponse.lat.getOrElse(0.0),
          longitude = apiResponse.lon.getOrElse(0.0),
          country = apiResponse.countryCode,
          city = apiResponse.city
        )

      } yield location

  private def isLocalIp(ip: String): Boolean =
    ip == "127.0.0.1" ||
      ip == "localhost" ||
      ip.startsWith("192.168.") ||
      ip.startsWith("10.") ||
      ip.startsWith("172.")

object IpApiGeoIpService:
  def make(client: Client): GeoIpService = IpApiGeoIpService(client)
