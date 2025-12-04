
ThisBuild / version := "0.1.0"

ThisBuild / scalaVersion := "3.7.4"

ThisBuild / sbtVersion := "1.11.7"

ThisBuild / organization := "vamotec"

ThisBuild / scalacOptions ++= Seq(
  "-encoding", "UTF-8",
  "-feature",
  "-deprecation",
  "-unchecked",
  "-Wconf:src=src_managed/.*:silent"
)

// 依赖版本
lazy val Versions = new {
  val zio = "2.1.23"
  val zioHttp = "3.7.0"
  val zioConfig = "4.0.6"
  val zioJson = "0.7.45"
  val zioLog = "2.5.2"
  val caliban = "2.11.1"
  val quill = "4.8.6"
  val flyway = "11.17.2"
  val postgresql = "42.7.8"
  val grpc = "1.77.0"
  val scalapb = "0.11.20"
  val kafka = "3.6.1"
  val jwt = "11.0.3"
  val prometheus = "0.16.0"
  val tapir = "1.12.5"
  val slick = "3.6.1"
  val amqp = "0.5.0"
}

lazy val codegen = project
  .in(file("modules/codegen"))
  .settings(
    name := "nexus-codegen",
    idePackagePrefix := Some("app.mosia.nexus"),
    Global / excludeLintKeys += idePackagePrefix,
    Compile / mainClass := Some("app.mosia.nexus.codegen.CodegenRunner"),
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % Versions.zio,
      "dev.zio" %% "zio-logging" % "2.5.1",
      "dev.zio" %% "zio-logging-slf4j" % "2.5.1",
      "dev.zio" %% "zio-config" % Versions.zioConfig,
      "dev.zio" %% "zio-config-typesafe" % Versions.zioConfig,
      "dev.zio" %% "zio-config-magnolia" % Versions.zioConfig,
      "io.github.cdimascio" % "dotenv-java" % "3.2.0",
      // slick codegen
      "com.typesafe.slick" %% "slick" % Versions.slick,
      "com.typesafe.slick" %% "slick-codegen" % Versions.slick,
      // 数据库
      "org.postgresql" % "postgresql" % Versions.postgresql,
    )
  )

lazy val migration = project
  .in(file("modules/migration"))
  .settings(
    name := "nexus-migration",
    idePackagePrefix := Some("app.mosia.nexus"),
    Global / excludeLintKeys += idePackagePrefix,
    Compile / mainClass := Some("app.mosia.nexus.migration.MigrationRunner"),
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % Versions.zio,
      "dev.zio" %% "zio-logging" % "2.5.1",
      "dev.zio" %% "zio-logging-slf4j" % "2.5.1",
      "dev.zio" %% "zio-config" % Versions.zioConfig,
      "dev.zio" %% "zio-config-typesafe" % Versions.zioConfig,
      "dev.zio" %% "zio-config-magnolia" % Versions.zioConfig,
      "org.projectlombok" % "lombok" % "1.18.42" % Provided,
      "io.github.cdimascio" % "dotenv-java" % "3.2.0",
      // 迁移
      "org.flywaydb" % "flyway-core" % Versions.flyway,
      "org.flywaydb" % "flyway-database-postgresql" % Versions.flyway,
      "com.zaxxer" % "HikariCP" % "7.0.2",
      // 数据库
      "org.postgresql" % "postgresql" % Versions.postgresql
    )
  )

lazy val nexus = project
  .in(file("modules/nexus"))
  .enablePlugins(JibPlugin)
  .settings(
    name := "nexus",
    idePackagePrefix := Some("app.mosia.nexus"),
    Global / excludeLintKeys += idePackagePrefix,
    jibBaseImage := "eclipse-temurin:25-jre",
    jibRegistry := "ghcr.io",
    jibTargetImageCredentialHelper := Some("docker-credential-desktop"),
    jibPlatforms := Set(JibPlatforms.arm64),
    jibName := "mosia-nexus",
    Compile / mainClass := Some("app.mosia.nexus.Main"),
    // 定义所有可用的 main classes
    Compile / discoveredMainClasses := Seq(
      "app.mosia.nexus.Main",
      "app.mosia.nexus.MainDebug",
      "app.mosia.nexus.MinimalMain"
    ),
    Compile / unmanagedSourceDirectories += baseDirectory.value / "src_managed" / "main",
    Compile / PB.targets := Seq(
      scalapb.gen(grpc = true) -> (Compile / sourceManaged).value / "scalapb",
      scalapb.zio_grpc.ZioCodeGenerator -> (Compile / sourceManaged).value / "scalapb"
    ),
    Compile / PB.includePaths += target.value / "protobuf_external",
    libraryDependencies ++= Seq(
      // 只依赖 ZIO 核心
      "dev.zio" %% "zio" % Versions.zio,
      "dev.zio" %% "zio-streams" % Versions.zio,

      // 测试
      "dev.zio" %% "zio-test" % Versions.zio % Test,
      "dev.zio" %% "zio-test-sbt" % Versions.zio % Test,
      "dev.zio" %% "zio-test-magnolia" % Versions.zio % Test,
      "org.testcontainers" % "postgresql" % "1.21.3" % Test,
      "dev.zio" %% "zio-http-testkit" % Versions.zioHttp % Test,

      "dev.zio" %% "zio-json" % Versions.zioJson,
      "org.mindrot" % "jbcrypt" % "0.4",

      // 数据库
      "io.getquill" %% "quill-jdbc-zio" % Versions.quill,
      "org.postgresql" % "postgresql" % Versions.postgresql,

      // gRPC
      "io.grpc" % "grpc-netty" % Versions.grpc,
      "com.thesamet.scalapb" %% "scalapb-runtime-grpc" %  scalapb.compiler.Version.scalapbVersion,
      "com.google.protobuf" % "protobuf-java" % "4.33.1" % "protobuf",

      // Redis
      "io.lettuce" % "lettuce-core" % "7.1.0.RELEASE",

      // RabbitMQ (用于外部通知)
      "nl.vroste" %% "zio-amqp" % Versions.amqp,

      // JavaMail (SMTP 邮件发送)
      "com.sun.mail" % "javax.mail" % "1.6.2",

      // JWT
      "com.github.jwt-scala" %% "jwt-zio-json" % Versions.jwt,

      // 监控
      "io.prometheus" % "simpleclient" % Versions.prometheus,
      "io.prometheus" % "simpleclient_hotspot" % Versions.prometheus,

      // 日志
      "dev.zio" %% "zio-logging" % Versions.zioLog,
      "dev.zio" %% "zio-logging-slf4j" % Versions.zioLog,
      "ch.qos.logback" % "logback-classic" % "1.5.21",

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
      "com.softwaremill.sttp.tapir"   %% "tapir-json-zio"          % Versions.tapir,

      // 配置
      "dev.zio" %% "zio-config" % Versions.zioConfig,
      "dev.zio" %% "zio-config-typesafe" % Versions.zioConfig,
      "dev.zio" %% "zio-config-magnolia" % Versions.zioConfig,
      "io.github.cdimascio" % "dotenv-java" % "3.2.0",
      "dev.zio" %% "zio-metrics-connectors-prometheus" % "2.5.4",

      // MacOS 需要
      "io.netty" % "netty-resolver-dns-native-macos" % "4.2.7.Final" classifier "osx-aarch_64"
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
  )

lazy val root = project
  .in(file("."))
  .aggregate(codegen, migration, nexus)
  .settings(
    publish := {},
    publishLocal := {},
  )

// 并行测试
Test / parallelExecution := true

addCommandAlias("fmt", "scalafmtAll")
addCommandAlias("fmtCheck", "scalafmtCheckAll")

addCommandAlias("nexus-debug", "nexus/runMain app.mosia.nexus.MainDebug")
addCommandAlias("nexus-minimal", "nexus/runMain app.mosia.nexus.MinimalMain")
addCommandAlias("nexus-run", "nexus/run")