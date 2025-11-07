package app.mosia.nexus

import app.mosia.nexus.application.service.audit.{AuditService, AuditServiceLive}
import app.mosia.nexus.application.service.auth.{AuthService, AuthServiceLive, OAuth2Service, OAuth2ServiceLive}
import app.mosia.nexus.application.service.device.{DeviceService, DeviceServiceLive}
import app.mosia.nexus.application.service.session.{SessionService, SessionServiceLive}
import app.mosia.nexus.application.service.user.{UserService, UserServiceLive}
import app.mosia.nexus.infra.auth.{JwtContent, JwtService, JwtServiceLive}
import app.mosia.nexus.infra.config.{AppConfig, HttpConfig}
import app.mosia.nexus.infra.messaging.kafka.KafkaProducerServiceLive
import app.mosia.nexus.infra.persistence.BaseSource
import app.mosia.nexus.infra.persistence.postgres.migration.FlywayService
import app.mosia.nexus.infra.persistence.postgres.repository.*
import app.mosia.nexus.infra.persistence.redis.RedisServiceLive
import app.mosia.nexus.presentation.graphql.GraphQLApi
import app.mosia.nexus.presentation.http.RESTApi
import io.github.cdimascio.dotenv.Dotenv
import zio.*
import zio.config.magnolia.deriveConfig
import zio.config.typesafe.TypesafeConfigProvider
import zio.http.{Client, Response, Routes, Server}
import zio.logging.backend.SLF4J

import javax.sql.DataSource

object Main extends ZIOAppDefault:
  // 在 bootstrap 中加载 .env
  override val bootstrap: URLayer[ZIOAppArgs, Dotenv] =
    ZLayer.fromZIO {
      ZIO.attempt {
        Dotenv
          .configure()
          .ignoreIfMissing()
          .systemProperties() // 自动设置到 java.lang.System properties
          .load()
      }.orDie
    } ++ Runtime.removeDefaultLoggers ++ SLF4J.slf4j

  val app: URIO[
    SessionService & UserService & AuditService & DeviceService & OAuth2Service & AuthService,
    Routes[JwtService, Response]
  ] =
    (for
      http <- RESTApi.make
      grapql <- GraphQLApi.make
    yield http ++ grapql).orDie

  override def run: ZIO[Any, Throwable, Unit] =
    (for {
      routes <- app
      httpConfig <- ZIO.service[HttpConfig]
      dataSource <- ZIO.service[DataSource]
      _ <- FlywayService(dataSource).migrate
      _ <- ZIO.logInfo(s"Server starting on http://${httpConfig.host}:${httpConfig.port}/health")
      _ <- ZIO.logInfo(s"GraphiQL UI available at http://${httpConfig.host}:${httpConfig.port}/graphiql")
      _ <- Server.serve(routes)
    } yield ())
      .provide(
        ZLayer.make[
          Server & HttpConfig & UserService & JwtContent & JwtService & DeviceService & AuditService & AuthService &
            OAuth2Service & SessionService & DataSource & Client
        ](
          // API 层

          // 业务逻辑层
          UserServiceLive.live,
          AuthServiceLive.live,
          OAuth2ServiceLive.live,
          SessionServiceLive.live,
          AuditServiceLive.live,
          DeviceServiceLive.live,

          // 核心服务层
          JwtServiceLive.live,
          JwtContent.live,

          // 集成层 (新增)
          KafkaProducerServiceLive.live,
          KafkaProducerServiceLive.producerLayer,
          RedisServiceLive.live,
          RedisServiceLive.singleNode,

          // 持久化层
          UserRepositoryLive.live,
          SessionRepositoryLive.live,
          DeviceRepositoryLive.live,
          AuditLogRepositoryLive.live,
          AuthenticatorRepositoryLive.live,
          RefreshTokenRepositoryLive.live,
          ChallengeRepositoryLive.live,
          DefaultDbContext.live,
          BaseSource.combineDataSource,

          // 基础设施与配置层
          Server.live,
          Client.default,
          ZLayer.service[AppConfig].project(_.http),
          ZLayer.service[AppConfig].project(_.auth),
          ZLayer.service[AppConfig].project(_.auth.token),
          ZLayer.service[AppConfig].project(_.kafka),
          ZLayer.service[AppConfig].project(_.device),
          ZLayer.fromZIO(
            ZIO.service[HttpConfig].map(c => Server.Config.default.binding(c.host, c.port))
          ),
          ZLayer.fromZIO(
            TypesafeConfigProvider.fromResourcePath().load(deriveConfig[AppConfig])
          )
        )
      )
