package app.mosia.nexus
package domain.model.network

/** 端口安全配置 */
case class PortSecurity(
  encryption: EncryptionRequirement = EncryptionRequirement.Optional,
  authentication: AuthenticationRequirement = AuthenticationRequirement.None,
  ipWhitelist: Set[String] = Set.empty, // IP白名单
  rateLimiting: Option[RateLimit] = None // 速率限制
)
