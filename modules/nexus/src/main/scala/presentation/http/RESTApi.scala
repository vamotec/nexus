package app.mosia.nexus
package presentation.http

import domain.config.AppConfig
import domain.services.app.*
import domain.services.infra.{HealthCheckService, JwtService, PrometheusExporter}
import presentation.http.endpoint.*
import presentation.http.websocket.WsRoutes

import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import zio.ZIO
import zio.http.*

object RESTApi:
  def makePublic: ZIO[
    PrometheusExporter & HealthCheckService & SessionService & AuditService & DeviceService & UserService &
      OAuth2Service & AuthService & AppConfig,
    Nothing,
    Routes[SignalingService & JwtService, Response]
  ] =
    for
      config <- ZIO.service[AppConfig]
      authService <- ZIO.service[AuthService]
      oauthService <- ZIO.service[OAuth2Service]
      userService <- ZIO.service[UserService]
      deviceService <- ZIO.service[DeviceService]
      auditService <- ZIO.service[AuditService]
      sessionService <- ZIO.service[SessionService]
      healthService <- ZIO.service[HealthCheckService]
      prometheus <- ZIO.service[PrometheusExporter]

      // Tapir endpoints
      tapirEndpoints = AuthEndpoint(
        authService,
        deviceService,
        auditService
      ).publicEndpoints ++ OAuth2Endpoint(
        oauthService,
        authService
      ).publicEndpoints ++ UserEndpoint(
        userService
      ).publicEndpoints ++ HealthEndpoint(
        config,
        healthService
      ).publicEndpoints ++ MetricsEndpoint(
        config,
        prometheus
      ).publicEndpoints

      // Convert Tapir endpoints to ZIO HTTP routes
      tapirRoutes <- ZIO.succeed(ZioHttpInterpreter().toHttp[Any](tapirEndpoints))

      // Combine all routes
      allRoutes = tapirRoutes
    yield allRoutes

  def makeSecure
    : ZIO[SignalingService & SessionService & UserService, Nothing, Routes[SignalingService & JwtService, Response]] =
    for
      userService <- ZIO.service[UserService]
      sessionService <- ZIO.service[SessionService]
      signalingService <- ZIO.service[SignalingService]

      // Tapir endpoints
      tapirEndpoints = UserEndpoint(
        userService
      ).secureEndpoints ++ WebRTCEndpoint(
        sessionService,
        signalingService
      ).secureEndpoints

      // Convert Tapir endpoints to ZIO HTTP routes
      tapirRoutes <- ZIO.succeed(ZioHttpInterpreter().toHttp[JwtService](tapirEndpoints))

      // Combine all routes
      allRoutes = tapirRoutes ++ WsRoutes.routes
    yield allRoutes
