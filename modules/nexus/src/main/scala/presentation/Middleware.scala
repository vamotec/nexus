package app.mosia.nexus
package presentation

import domain.services.infra.JwtContent
import domain.model.jwt.JwtPayload
import domain.services.infra.JwtService
import domain.config.AppConfig

import zio.{Duration, NonEmptyChunk, UIO, ZIO}
import zio.http.Middleware.CorsConfig
import zio.http.*
import zio.json.DecoderOps

object Middleware:
  /** 将 domain.config.CorsConfig 转换为 zio.http.Middleware.CorsConfig */
  def corsConfig(config: AppConfig): CorsConfig =
    CorsConfig(
      allowedOrigin = origin =>
        if config.cors.enabled then
          config.cors.allowedOrigins match
            case List("*") => Some(Header.AccessControlAllowOrigin.All)
            case origins =>
              val originStr = origin.toString
              if origins.contains(originStr) then Some(Header.AccessControlAllowOrigin.Specific(origin))
              else None
        else None,
      allowedMethods =
        if config.cors.allowedMethods.contains("*") then Header.AccessControlAllowMethods.All
        else
          Header.AccessControlAllowMethods(
            config.cors.allowedMethods.map(m => Method.fromString(m))*
          )
      ,
      allowedHeaders =
        if config.cors.allowedHeaders.contains("*") then Header.AccessControlAllowHeaders.All
        else Header.AccessControlAllowHeaders(config.cors.allowedHeaders*),
      allowCredentials =
        if config.cors.allowCredentials then Header.AccessControlAllowCredentials.Allow
        else Header.AccessControlAllowCredentials.DoNotAllow,
      exposedHeaders =
        if config.cors.exposedHeaders.contains("*") then Header.AccessControlExposeHeaders.All
        else if (config.cors.exposedHeaders.nonEmpty)
          Header.AccessControlExposeHeaders.Some(
            NonEmptyChunk.fromIterable(config.cors.exposedHeaders.head, config.cors.exposedHeaders.tail)
          )
        else
          Header.AccessControlExposeHeaders.None,
      maxAge = Some(Header.AccessControlMaxAge(Duration.fromSeconds(config.cors.maxAge.toLong)))
    )
  // 这是一个 ZIO HTTP 中间件，它会拦截请求
  // 如果请求包含有效的 JWT, 它会将 JWT 的内容 (JwtContent) 注入到 ZIO 环境中，然后交由被它包裹的路由处理器处理
  // 如果 JWT 无效或不存在, 它会直接返回 401 Unauthorized 错误
  private lazy val isDev: Boolean = {
    val env    = sys.env.getOrElse("APP_ENV", sys.props.getOrElse("APP_ENV", "production"))
    val result = env == "development"
    println(s"[AuthMiddleware] Running in ${if result then "DEVELOPMENT" else "PRODUCTION"} mode (APP_ENV=$env)")
    result
  }

  private val logDevModeOnce: UIO[Unit] =
    ZIO
      .logWarning("DEV MODE: Authentication bypassed for development")
      .once
      .map(_ => ())

  private val devPayload = JwtPayload(
    userIdStr = "550e8400-e29b-41d4-a716-446655440000"
  )

  val auth: Middleware[JwtService & JwtContent] = new Middleware:
    override def apply[Env1 <: JwtService & JwtContent, Err](routes: Routes[Env1, Err]): Routes[Env1, Err] =
      routes.transform: handler =>
        Handler.scoped:
          Handler.fromFunctionZIO[Request]: request =>
            request.header(Header.Authorization) match
              case Some(Header.Authorization.Bearer(token)) =>
                // 有 token，正常验证
                for
                  jwtService <- ZIO.service[JwtService]
                  claim <- jwtService
                    .decode(token.value.toArray.mkString)
                    .mapError(_ => Response.unauthorized("Invalid token"))
                  ctx <- ZIO.service[JwtContent]
                  payload <- ZIO
                    .fromEither(claim.content.fromJson[JwtPayload])
                    .mapError(e => Response.internalServerError(s"Failed to decode claim content: $e"))
                  _ <- ctx.set(payload)
                  response <- handler(request)
                yield response

              case None if isDev =>
                // 开发环境 + 无 token = 使用假的 payload
                for
                  _ <- logDevModeOnce
                  ctx <- ZIO.service[JwtContent]
                  _ <- ctx.set(devPayload)
                  response <- handler(request)
                yield response

              case _ =>
                ZIO.fail(Response.unauthorized("Missing or invalid Authorization header"))
