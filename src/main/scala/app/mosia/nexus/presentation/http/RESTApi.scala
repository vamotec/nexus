package app.mosia.nexus.presentation.http

import app.mosia.nexus.application.service.audit.AuditService
import app.mosia.nexus.application.service.auth.{AuthService, OAuth2Service}
import app.mosia.nexus.application.service.device.DeviceService
import app.mosia.nexus.infra.auth.JwtService
import app.mosia.nexus.presentation.http.endpoint.{AuthEndpoint, OAuth2Endpoint}
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import zio.ZIO
import zio.http.{Response, Routes}

object RESTApi:
  def make: ZIO[AuditService & DeviceService & OAuth2Service & AuthService, Nothing, Routes[JwtService, Response]] =
    for
      authService <- ZIO.service[AuthService]
      oauthService <- ZIO.service[OAuth2Service]
      deviceService <- ZIO.service[DeviceService]
      auditService <- ZIO.service[AuditService]
      endpoints = AuthEndpoint(authService, deviceService, auditService).endpoints ++ OAuth2Endpoint(
        oauthService,
        authService
      ).endpoints
      routes <- ZIO.succeed(ZioHttpInterpreter().toHttp[JwtService](endpoints))
    yield routes
