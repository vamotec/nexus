ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.7.3"

ThisBuild / sbtVersion := "1.11.7"

// 通用配置
lazy val commonSettings = Seq(
  organization := "app.mosia",
  scalacOptions ++= Seq(
    "-deprecation",
    "-feature",
    "-unchecked",
    "-Xfatal-warnings"
  ),
  testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
)

// 依赖版本
lazy val Versions = new {
  val zio = "2.1.22"
  val zioHttp = "3.5.1"
  val zioConfig = "4.0.5"
  val zioJson = "0.7.45"
  val caliban = "2.11.1"
  val quill = "4.8.6"
  val flyway = "11.15.0"
  val postgresql = "42.7.8"
  val grpc = "1.76.0"
  val scalapb = "0.11.20"
  val redis = "1.1.8"
  val kafka = "3.6.1"
  val jwt = "11.0.3"
  val prometheus = "0.16.0"
  val tapir = "1.12.1"
}

// ============ 领域层 (纯业务逻辑) ============
lazy val domain = project
  .in(file("modules/domain"))
  .settings(
    commonSettings,
    name := "nexus-domain",
    libraryDependencies ++= Seq(
      // 只依赖 ZIO 核心
      "dev.zio" %% "zio" % Versions.zio,
      "dev.zio" %% "zio-streams" % Versions.zio,

      // 测试
      "dev.zio" %% "zio-test" % Versions.zio % Test,
      "dev.zio" %% "zio-test-sbt" % Versions.zio % Test
    )
  )

// ============ 应用层 (用例编排) ============
lazy val application = project
  .in(file("modules/application"))
  .dependsOn(domain)
  .settings(
    commonSettings,
    name := "nexus-application",
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % Versions.zio,
      "dev.zio" %% "zio-json" % Versions.zioJson,
      "org.mindrot" % "jbcrypt" % "0.4",

      // 测试
      "dev.zio" %% "zio-test" % Versions.zio % Test,
      "dev.zio" %% "zio-test-sbt" % Versions.zio % Test,
      "dev.zio" %% "zio-test-magnolia" % Versions.zio % Test
    )
  )

// ============ 基础设施层 (技术实现) ============
lazy val infrastructure = project
  .in(file("modules/infrastructure"))
  .dependsOn(domain)
  .settings(
    commonSettings,
    name := "nexus-infrastructure",
    libraryDependencies ++= Seq(
      // 数据库
      "io.getquill" %% "quill-jdbc-zio" % Versions.quill,
      "org.postgresql" % "postgresql" % Versions.postgresql,
      "org.flywaydb" % "flyway-core" % Versions.flyway,
      "org.flywaydb" % "flyway-database-postgresql" % Versions.flyway,

      // gRPC
      "io.grpc" % "grpc-netty" % Versions.grpc,
      "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % Versions.scalapb,
      "com.thesamet.scalapb" %% "scalapb-runtime" % Versions.scalapb % "protobuf",

      // Redis
      "dev.zio" %% "zio-redis" % Versions.redis,

      // Kafka
      "dev.zio" %% "zio-kafka" % "3.1.0",

      // JWT
      "com.github.jwt-scala" %% "jwt-zio-json" % Versions.jwt,

      // 监控
      "io.prometheus" % "simpleclient" % Versions.prometheus,
      "io.prometheus" % "simpleclient_hotspot" % Versions.prometheus,

      // 日志
      "dev.zio" %% "zio-logging" % "2.5.1",
      "dev.zio" %% "zio-logging-slf4j" % "2.5.1",
      "ch.qos.logback" % "logback-classic" % "1.5.20",

      // 测试
      "dev.zio" %% "zio-test" % Versions.zio % Test,
      "org.testcontainers" % "postgresql" % "1.21.3" % Test
    ),
    // gRPC 代码生成
    Compile / PB.targets := Seq(
      scalapb.gen(grpc = true) -> (Compile / sourceManaged).value / "scalapb",
      scalapb.zio_grpc.ZioCodeGenerator -> (Compile / sourceManaged).value / "scalapb"
    ),
    Compile / PB.includePaths += target.value / "protobuf_external",
    PB.protocVersion := "3.25.1"
  )

// ============ 表现层 (API 接口) ============
lazy val presentation = project
  .in(file("modules/presentation"))
  .dependsOn(application, infrastructure)
  .settings(
    commonSettings,
    name := "nexus-presentation",
    libraryDependencies ++= Seq(
      // ZIO HTTP
      "dev.zio" %% "zio-http" % Versions.zioHttp,

      // GraphQL (Caliban)
      "com.github.ghostdogpr" %% "caliban" % Versions.caliban,
      "com.github.ghostdogpr" %% "caliban-zio-http" % Versions.caliban,

      // JSON
      "dev.zio" %% "zio-json" % Versions.zioJson,
      "com.softwaremill.sttp.tapir"   %% "tapir-core"              % Versions.tapir,
      "com.softwaremill.sttp.tapir"   %% "tapir-zio"               % Versions.tapir,
      "com.softwaremill.sttp.tapir"   %% "tapir-zio-http-server"   % Versions.tapir,
      "com.softwaremill.sttp.tapir"   %% "tapir-swagger-ui-bundle" % Versions.tapir,
      "com.softwaremill.sttp.tapir"   %% "tapir-json-zio"          % Versions.tapir,

      // 配置
      "dev.zio" %% "zio-config" % Versions.zioConfig,
      "dev.zio" %% "zio-config-typesafe" % Versions.zioConfig,
      "dev.zio" %% "zio-config-magnolia" % Versions.zioConfig,
      "io.github.cdimascio" % "dotenv-java" % "3.2.0",

      // 测试
      "dev.zio" %% "zio-test" % Versions.zio % Test,
      "dev.zio" %% "zio-http-testkit" % Versions.zioHttp % Test
    )
  )

// ============ 根项目 ============
lazy val root = (project in file("."))
  .aggregate(domain, application, infrastructure, presentation)
  .dependsOn(presentation)
  .settings(
    commonSettings,
    name := "nexus",

    // 打包配置
    assembly / mainClass := Some("app.mosia.nexus.Main"),
    assembly / assemblyJarName := "nexus.jar",
    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", xs @ _*) => xs match {
        case "MANIFEST.MF" :: Nil => MergeStrategy.discard
        case "services" :: _ => MergeStrategy.concat
        case _ => MergeStrategy.discard
      }
      case "application.conf" => MergeStrategy.concat
      case "reference.conf" => MergeStrategy.concat
      case _ => MergeStrategy.first
    },

    // Docker 配置
//    Docker / packageName := "mosia/nexus",
//    Docker / version := version.value,
//    Docker / dockerExposedPorts := Seq(8080, 8081, 9090),
//    Docker / dockerBaseImage := "eclipse-temurin:25-jre-alpine"
  )

// ============ 全局设置 ============

// 代码格式化
scalafmtOnCompile := true

// 并行测试
Test / parallelExecution := true

addCommandAlias("fmt", "scalafmtAll")
addCommandAlias("fmtCheck", "scalafmtCheckAll")