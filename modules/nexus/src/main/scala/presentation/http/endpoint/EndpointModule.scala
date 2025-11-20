package app.mosia.nexus
package presentation.http.endpoint

import domain.services.infra.JwtService

import sttp.capabilities.zio.ZioStreams
import sttp.model.StatusCode
import sttp.tapir.{Endpoint, EndpointOutput}
import sttp.tapir.json.zio.jsonBody
import sttp.tapir.ztapir.*
import zio.ZIO

trait EndpointModule:
  def publicEndpoints: List[ZServerEndpoint[Any, ZioStreams]]
  def secureEndpoints: List[ZServerEndpoint[JwtService, ZioStreams]]
