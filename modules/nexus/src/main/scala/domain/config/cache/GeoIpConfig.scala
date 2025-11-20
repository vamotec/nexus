package app.mosia.nexus
package domain.config.cache

import zio.*

case class GeoIpConfig(cacheTtl: Duration = 1.hour, cacheMaxSize: Int = 10000)
