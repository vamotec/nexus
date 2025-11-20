package app.mosia.nexus
package domain.config.cache

import domain.config.cache.GeoIpConfig

case class CacheConfig(cacheEnabled: Boolean = true, geoip: GeoIpConfig)
