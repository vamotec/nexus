package app.mosia.nexus

import infrastructure.geoip.*
import infrastructure.jwt.JwtServiceLive
import infrastructure.messaging.{DomainEventPublisherLive, KafkaProducerServiceLive}
import infrastructure.monitoring.{HealthCheckServiceLive, PrometheusExporterLive, SystemMetricsCollectorLive}
import infrastructure.neuro.*
import infrastructure.persistence.BaseSource
import infrastructure.persistence.postgres.repository.*
import infrastructure.persistence.timescale.repository.{SessionMetricsRepositoryLive, TimescaleDbContext}
import infrastructure.redis.RedisServiceLive
import infrastructure.resource.ResourceAllocationServiceLive
import application.services.*
import domain.config.{AppConfig, HttpConfig}
import domain.services.app.*
import domain.services.infra.*
import presentation.Middleware
import presentation.graphql.GraphQLApi
import presentation.http.RESTApi

import io.github.cdimascio.dotenv.Dotenv
import zio.*
import zio.config.magnolia.deriveConfig
import zio.config.typesafe.TypesafeConfigProvider
import zio.http.*
import zio.http.Middleware.cors
import zio.logging.backend.SLF4J
import zio.metrics.connectors.prometheus.*

object Main extends ZIOAppDefault:
  // 在 bootstrap 中加载 .env
  override val bootstrap: ZLayer[ZIOAppArgs, Any, Unit] =
    ZLayer.fromZIO {
      for {
        // 1. 先加载环境变量
        _ <- ZIO.attempt {
          Dotenv
            .configure()
            .ignoreIfMissing()
            .systemProperties()
            .load()
        }.orDie
        // 2. 可选：验证关键环境变量
        _ <- ZIO.logInfo("Environment variables loaded")
      } yield ()
    } ++ (Runtime.removeDefaultLoggers ++ SLF4J.slf4j)

  val app: URIO[
    AppConfig & JwtContent & ProjectService & SimulationService & SessionService & TrainingService & UserService &
      AuditService & DeviceService & OAuth2Service & AuthService & SessionService & SignalingService &
      HealthCheckService & PrometheusExporter,
    Routes[JwtService & JwtContent & JwtService & SignalingService & HealthCheckService & PrometheusExporter, Response]
  ] =
    (for
      appConfig <- ZIO.service[AppConfig]
      public <- RESTApi.makePublic
      secure <- RESTApi.makeSecure
      grapql <- GraphQLApi.make

      // 业务路由（需要认证和CORS）
      business = (secure ++ grapql) @@ cors(Middleware.corsConfig(appConfig)) @@ Middleware.auth

      // 合并所有路由
      combine = public ++ business
    yield combine).orDie

  override def run: ZIO[ZIOAppArgs & Scope, Any, Any] =
    (for {
      routes <- app
      appConfig <- ZIO.service[AppConfig]

      // 启动系统指标收集
      metricsCollector <- ZIO.service[SystemMetricsCollector]
      _ <- metricsCollector.startCollection().fork

      // 启动日志
      _ <- ZIO.logInfo(s"Server starting on http://${appConfig.http.host}:${appConfig.http.port}")
      _ <- ZIO.logInfo(
        s"Health check: http://${appConfig.http.host}:${appConfig.http.port}${appConfig.monitoring.healthCheck.path}"
      )
      _ <- ZIO.logInfo(
        s"Prometheus metrics: http://${appConfig.http.host}:${appConfig.http.port}${appConfig.monitoring.prometheus.path}"
      )
      _ <- ZIO.logInfo(s"GraphiQL UI: http://${appConfig.http.host}:${appConfig.http.port}/graphiql")

      // 启动 HTTP 服务器
      _ <- Server.serve(routes)
    } yield ())
      .provide(
        ZLayer.make[
          Server & AppConfig & UserService & JwtContent & JwtService & SessionService & DeviceService & AuditService &
            AuthService & OAuth2Service & Client & SignalingService & ProjectService & SimulationService &
            TrainingService & HealthCheckService & PrometheusExporter & PrometheusPublisher & SystemMetricsCollector
        ](
          // 业务逻辑层
          UserServiceLive.live,
          AuthServiceLive.live,
          OAuth2ServiceLive.live,
          SessionServiceLive.live,
          SimulationServiceLive.live,
          ProjectServiceLive.live,
          AuditServiceLive.live,
          DeviceServiceLive.live,
          SignalingServiceLive.live,
          TrainingServiceLive.live,

          // 核心服务层
          JwtServiceLive.live,
          JwtContent.live,
          NeuroClientLive.live,
          CachedGeoIpService.geoIpLayer,
          NeuroConnectionManager.live,
          SmartRoutingStrategy.live,
          ClusterRegistryLive.live,
          ResourceAllocationServiceLive.live,

          // 监控层
          HealthCheckServiceLive.live,
          PrometheusExporterLive.live,
          SystemMetricsCollectorLive.live,
          prometheusLayer,
          publisherLayer,
          SystemMetricsCollectorLive.config,

          // 集成层
          KafkaProducerServiceLive.live,
          KafkaProducerServiceLive.producerLayer,
          RedisServiceLive.live,
          RedisServiceLive.singleNode,
          DomainEventPublisherLive.live,

          // 持久化层
          UserRepositoryLive.live,
          SessionRepositoryLive.live,
          SimulationRepositoryLive.live,
          ProjectRepositoryLive.live,
          SessionMetricsRepositoryLive.live,
          DeviceRepositoryLive.live,
          AuditLogRepositoryLive.live,
          AuthenticatorRepositoryLive.live,
          RefreshTokenRepositoryLive.live,
          ChallengeRepositoryLive.live,
          TrainingRepositoryLive.live,
          DefaultDbContext.live,
          TimescaleDbContext.live,
          BaseSource.allDataSources,

          // 基础设施与配置层
          Server.live,
          Client.default,
          ZLayer.fromZIO(
            ZIO.service[AppConfig].map(c => Server.Config.default.binding(c.http.host, c.http.port))
          ),
          ZLayer.fromZIO(
            TypesafeConfigProvider
              .fromResourcePath()
              .nested("app")
              .load(deriveConfig[AppConfig])
          )
        )
      )
