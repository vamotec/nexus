package app.mosia.nexus
package domain.services.infra

import domain.config.neuro.GeoLocation
import domain.error.{AppError, AppTask}
import zio.json.*
import zio.*

trait GeoIpService:
  def lookup(ipAddress: String): ZIO[Scope, AppError, GeoLocation]
//  def lookupBatch(ipAddresses: List[String]): AppTask[List[(String, GeoLocation)]]
