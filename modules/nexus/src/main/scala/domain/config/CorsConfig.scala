package app.mosia.nexus
package domain.config

/** CORS 跨域配置 */
case class CorsConfig(
  enabled: Boolean,
  allowedOrigins: List[String],
  allowedMethods: List[String],
  allowedHeaders: List[String],
  exposedHeaders: List[String],
  maxAge: Int,
  allowCredentials: Boolean
)
