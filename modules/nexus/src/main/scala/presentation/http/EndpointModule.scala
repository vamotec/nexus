package app.mosia.nexus
package presentation.http

import domain.services.infra.JwtContent

import sttp.capabilities.zio.ZioStreams
import sttp.tapir.ztapir.*

trait EndpointModule:
  def publicEndpoints: List[ZServerEndpoint[Any, ZioStreams]]
  def secureEndpoints: List[ZServerEndpoint[JwtContent, ZioStreams]]
