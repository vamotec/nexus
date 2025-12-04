package app.mosia.nexus

import application.services.*
import domain.config.{AppConfig, HttpConfig}
import domain.services.app.*
import domain.services.infra.*
import infrastructure.cloud.*
import infrastructure.geoip.*
import infrastructure.jwt.JwtServiceLive
import infrastructure.messaging.HybridEventPublisher
import infrastructure.messaging.consumer.*
import infrastructure.monitoring.*
import infrastructure.notification.{AliyunSmsService, SmtpEmailService}
import infrastructure.persistence.*
import infrastructure.rabbitmq.RabbitMQServiceLive
import infrastructure.redis.LettuceRedis
import infrastructure.resource.ResourceAllocationServiceLive
import infrastructure.verification.VerificationCodeServiceLive
import presentation.Middleware
import presentation.http.RESTApi
import presentation.websocket.WsRoutes

import io.github.cdimascio.dotenv.Dotenv
import zio.config.magnolia.deriveConfig
import zio.config.typesafe.TypesafeConfigProvider
import zio.http.*
import zio.http.Middleware.cors
import zio.logging.backend.SLF4J
import zio.metrics.connectors.prometheus.*
import zio.{Scope, URIO, ZIO, ZIOAppArgs, ZIOAppDefault, ZLayer, durationInt, Runtime as ZioRuntime}

object Main extends ZIOAppDefault:
  // 在 bootstrap 中加载 .env
  override val bootstrap: ZLayer[ZIOAppArgs, Any, Unit] =
    ZLayer.fromZIO {
      for {
        _ <- ZIO.attempt {
          Dotenv
            .configure()
            .ignoreIfMissing()
            .systemProperties()
            .load()
        }.orDie
      } yield ()
    } ++ (ZioRuntime.removeDefaultLoggers ++ SLF4J.slf4j)

  def app(config: AppConfig): URIO[JwtContent & ProjectService & SimulationService & SessionService &
    TrainingService & UserService & SignalingService & SessionService & UserService & OrganizationService &
    PrometheusExporter & HealthCheckService & SessionService & AuditService & DeviceService & UserService &
    OAuth2Service & AuthService & NotificationService, Routes[JwtService & JwtContent & SignalingService &
    JwtService, Response]] =
    for
      public <- RESTApi.makePublic(config)
      secure <- RESTApi.makeSecure
      ws      = WsRoutes.routes
      // 业务路由（需要认证和CORS）
      httpRoute = ws @@ cors(Middleware.corsConfig(config))
      business = (secure ++ httpRoute) @@ Middleware.auth
      // 合并所有路由
      combine = public ++ business
    yield combine

  override def run: ZIO[ZIOAppArgs & Scope, Any, Any] =
    (for {
      _ <- ZIO.succeed(println("\n=== RUN START ==="))
      appConfig <- ZIO.service[AppConfig]
      routes <- app(appConfig)
      // 启动系统指标收集
      metricsCollector <- ZIO.service[SystemMetricsCollector]
      _ <- metricsCollector.startCollection().forkScoped

      // 启动 Redis Streams 消费者
      sessionConsumer <- ZIO.service[SessionEventConsumer]
      _ <- sessionConsumer.start.forkScoped

      trainingConsumer <- ZIO.service[TrainingEventConsumer]
      _ <- trainingConsumer.start.forkScoped

      // 启动 Streams 监控和自动清理
      monitor <- ZIO.service[StreamsMonitorService]
      _ <- monitor.startAutoCleanup.forkScoped

      // 启动 Outbox 处理器
      outboxProcessor <- ZIO.service[OutboxProcessor]
      _ <- outboxProcessor.start.forkScoped

      // 启动 RabbitMQ 消费者
      emailConsumer <- ZIO.service[EmailNotificationConsumer]
      _ <- emailConsumer.start.forkScoped

      smsConsumer <- ZIO.service[SmsNotificationConsumer]
      _ <- smsConsumer.start.forkScoped

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
          Scope & Server & AppConfig & UserService & JwtContent & JwtService & SessionService & DeviceService & AuditService &
            AuthService & OAuth2Service & Client & SignalingService & ProjectService & SimulationService & TrainingService & 
            HealthCheckService & PrometheusExporter & PrometheusPublisher & SystemMetricsCollector & OrganizationService & 
            SessionEventConsumer & TrainingEventConsumer & OutboxProcessor & StreamsMonitorService & RabbitMQService & 
            EmailNotificationConsumer & EmailService & SmsNotificationConsumer & SmsService & NotificationService
        ](
          Scope.default,
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
          OrganizationServiceLive.live,
          NotificationServiceLive.live,
          // 核心服务层
          JwtServiceLive.live,
          JwtContent.live,
          NeuroClientLive.layer,
          CachedGeoIpService.geoIpLayer,
          SmartRoutingStrategy.live,
          ClusterRegistryLive.live,
          ResourceAllocationServiceLive.live,
          VerificationCodeServiceLive.live,
          // 监控层
          HealthCheckServiceLive.live,
          PrometheusExporterLive.live,
          SystemMetricsCollectorLive.live,
          StreamsMonitorService.live,
          prometheusLayer,
          publisherLayer,
          SystemMetricsCollectorLive.config,
          // 集成层（Redis Streams + PostgreSQL Outbox + RabbitMQ + Email + SMS）
          LettuceRedis.live,
          RabbitMQServiceLive.live,
          HybridEventPublisher.eventLive,  // 智能路由（默认使用这个）
          SessionEventConsumer.live,
          TrainingEventConsumer.live,
          EmailNotificationConsumer.live,
          SmsNotificationConsumer.live,
          OutboxProcessor.live,
          SmtpEmailService.live,
          AliyunSmsService.live,
          // 持久化层
          UserRepositoryLive.live,
          SessionRepositoryLive.live,
          SimulationRepositoryLive.live,
          ProjectRepositoryLive.live,
          DeviceRepositoryLive.live,
          AuditLogRepositoryLive.live,
          AuthenticatorRepositoryLive.live,
          RefreshTokenRepositoryLive.live,
          ChallengeRepositoryLive.live,
          TrainingRepositoryLive.live,
          OrganizationRepositoryLive.live,
          OrganizationMemberRepositoryLive.live,
          OAuthProviderRepositoryLive.live,
          OutboxRepositoryLive.live,  // Outbox 仓储
          DefaultDbContext.live,
          BaseSource.postgresLive,
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
      .ensuring(
        ZIO.succeed {
          // 强制终止 JVM，避免等待 SBT launcher 的非 daemon 线程池
          // 这些线程来自 sbt-launch.jar 的 ipcsocket 库，无法从应用层控制
          Runtime.getRuntime.halt(0)
        }.delay(500.millis)
      )
