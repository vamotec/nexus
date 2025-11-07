// ide插件
addSbtPlugin("org.jetbrains.scala" % "sbt-ide-settings" % "1.1.2")

// 代码格式化
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.2")

// 打包插件
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "2.1.5")

// Protobuf/gRPC 代码生成
addSbtPlugin("com.thesamet" % "sbt-protoc" % "1.0.8")

// 测试覆盖率
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "2.0.9")

// Docker 打包
addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.9.16")

// 依赖更新检查
addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.6.4")

// 依赖树可视化
addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.10.0-RC1")


libraryDependencies ++= Seq(
  "com.thesamet.scalapb" %% "compilerplugin" % "0.11.20",
  "com.thesamet.scalapb.zio-grpc" %% "zio-grpc-codegen" % "0.6.3",
)