package app.mosia.nexus
package domain.config.cloud

import zio.Duration
import zio.json.*

final case class GeoLocation(
  latitude: Double,
  longitude: Double,
  country: Option[String] = None,
  city: Option[String] = None,
  region: Option[String] = None,
  postalCode: Option[String] = None,
  timezone: Option[String] = None
) derives JsonCodec
