package app.mosia.nexus
package presentation.http

import domain.config.AppConfig
import domain.services.app.*
import domain.services.infra.*
import presentation.http.v1.*

import sttp.model.{Method, StatusCode}
import sttp.tapir.server.interceptor.cors.CORSConfig.*
import sttp.tapir.server.interceptor.cors.{CORSConfig, CORSInterceptor}
import sttp.tapir.server.ziohttp.{ZioHttpInterpreter, ZioHttpServerOptions}
import zio.http.*
import zio.{Task, ZIO}

object RESTApi:
  private val allowedDomains = Set(
    "http://localhost:3000",
    "https://staging.mosia.app",
    "https://mosia.app"
  )

  private val corsInterceptor: CORSInterceptor[Task] = CORSInterceptor.customOrThrow[Task](
    CORSConfig(
      allowedOrigin = AllowedOrigin.Matching(origin => allowedDomains.contains(origin)),
      allowedCredentials = AllowedCredentials.Allow,
      allowedMethods = AllowedMethods.Some(Set(Method.GET, Method.HEAD, Method.POST, Method.PUT, Method.DELETE)),
      allowedHeaders = AllowedHeaders.Reflect,
      exposedHeaders = ExposedHeaders.None,
      maxAge = MaxAge.Default,
      preflightResponseStatusCode = StatusCode.NoContent
    )
  )

  private val serverOptions: ZioHttpServerOptions[Any] = ZioHttpServerOptions
    .customiseInterceptors
    .corsInterceptor(corsInterceptor)
    .options
  
  def makePublic(config: AppConfig): ZIO[PrometheusExporter & HealthCheckService & SessionService & 
    NotificationService & AuditService & DeviceService & UserService & OAuth2Service & AuthService, 
    Nothing, Routes[Any, Response]] =
    for
      authService <- ZIO.service[AuthService]
      oauthService <- ZIO.service[OAuth2Service]
      userService <- ZIO.service[UserService]
      deviceService <- ZIO.service[DeviceService]
      auditService <- ZIO.service[AuditService]
      notificationService <- ZIO.service[NotificationService]
      sessionService <- ZIO.service[SessionService]
      healthService <- ZIO.service[HealthCheckService]
      prometheus <- ZIO.service[PrometheusExporter]

      // Tapir endpoints
      tapirEndpoints = AuthEndpoint(
        authService,
        deviceService,
        auditService,
        notificationService,
        userService
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
      tapirRoutes <- ZIO.succeed(ZioHttpInterpreter(serverOptions).toHttp[Any](tapirEndpoints))
    yield tapirRoutes

  def makeSecure
    : ZIO[SignalingService & SessionService & UserService & OrganizationService, Nothing, Routes[SignalingService & JwtContent, Response]] =
    for
      userService <- ZIO.service[UserService]
      sessionService <- ZIO.service[SessionService]
      signalingService <- ZIO.service[SignalingService]
      organizationService <- ZIO.service[OrganizationService]

      // Tapir endpoints
      tapirEndpoints = UserEndpoint(
        userService
      ).secureEndpoints ++ WebRTCEndpoint(
        sessionService,
        signalingService
      ).secureEndpoints ++ OrganizationEndpoint(
        organizationService
      ).secureEndpoints

      // Convert Tapir endpoints to ZIO HTTP routes
      tapirRoutes <- ZIO.succeed(ZioHttpInterpreter(serverOptions).toHttp[JwtContent](tapirEndpoints))
    yield tapirRoutes
