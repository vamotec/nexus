package app.mosia.nexus
package application.dto.response

import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

final case class IpApiResponse(
  status: String,
  country: Option[String],
  countryCode: Option[String],
  city: Option[String],
  lat: Option[Double],
  lon: Option[Double],
  timezone: Option[String]
) derives JsonDecoder
