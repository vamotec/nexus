package app.mosia.nexus.presentation.http.middleware

import zio.durationInt
import zio.http.Header
import zio.http.Middleware.CorsConfig

object CorsMiddleware:
  val corsConfig: CorsConfig =
    CorsConfig(
      allowedOrigin = _ => Some(Header.AccessControlAllowOrigin.All), // 允许任意 Origin
      allowedMethods = Header.AccessControlAllowMethods.All, // 允许所有方法
      allowedHeaders = Header.AccessControlAllowHeaders.All, // 允许所有 Header（包括 X-Market）
      allowCredentials = Header.AccessControlAllowCredentials.Allow, // 允许携带 cookies / headers
      exposedHeaders = Header.AccessControlExposeHeaders.All, // 允许前端访问所有响应头
      maxAge = Some(Header.AccessControlMaxAge(1.hour)) // 预检请求缓存1小时
    )
