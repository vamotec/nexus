package app.mosia.nexus
package domain.model.storage

import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

/** 网络挂载配置 */
case class NetworkMountConfig(
  server: String, // 服务器地址
  share: String, // 共享路径
  credentials: NetworkCredentials, // 认证信息
  cache: Boolean = true, // 是否启用缓存
  timeout: Duration = Duration.fromSeconds(30)
)
