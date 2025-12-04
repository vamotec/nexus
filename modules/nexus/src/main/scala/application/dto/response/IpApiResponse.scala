package app.mosia.nexus
package application.dto.response

import zio.*
import zio.json.*

final case class IpApiResponse(
  status: String,
  country: Option[String],
  countryCode: Option[String],
  city: Option[String],
  lat: Option[Double],
  lon: Option[Double],
  timezone: Option[String]
) derives JsonDecoder
