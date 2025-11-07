package app.mosia.nexus.presentation.http.endpoint

import app.mosia.nexus.infra.auth.JwtService
import sttp.capabilities.zio.ZioStreams
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.ztapir.ZServerEndpoint
import zio.Task

trait EndpointModule:
  def endpoints: List[ZServerEndpoint[JwtService, ZioStreams]]
//  def endpoints: List[ServerEndpoint[ZioStreams, Task]]
