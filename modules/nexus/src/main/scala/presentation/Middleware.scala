package app.mosia.nexus
package presentation

import domain.config.AppConfig
import domain.model.jwt.Payload
import domain.model.jwt.TokenType.*
import domain.services.infra.{JwtContent, JwtService}

import zio.http.*
import zio.http.Middleware.CorsConfig
import zio.json.DecoderOps
import zio.{Duration, NonEmptyChunk, ZIO}

object Middleware:
  def corsConfig(config: AppConfig): CorsConfig =
    // 预解析允许的 origins
    val parsedOrigins = config.cors.allowedOrigins.flatMap { originStr =>
      Header.Origin.parse(originStr) match {
        case Right(origin) => Some(origin)
        case Left(err) =>
          println(s"[CORS] Failed to parse origin '$originStr': $err")
          None
      }
    }

    CorsConfig(
      allowedOrigin = origin =>
        val originStr = Header.Origin.render(origin)
        println(s"[CORS] Request from: $originStr")
        println(s"[CORS] Allowed origins: ${config.cors.allowedOrigins}")

        if config.cors.enabled then
          config.cors.allowedOrigins match
            case List("*") => Some(Header.AccessControlAllowOrigin.All)
            case _ =>
              val allowed = parsedOrigins.contains(origin)
              println(s"[CORS] Origin allowed: $allowed")
              if allowed then
                Some(Header.AccessControlAllowOrigin.Specific(origin))
              else
                None
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

  val auth: Middleware[JwtService & JwtContent] = new Middleware:
    override def apply[Env1 <: JwtService & JwtContent, Err](
                                                              routes: Routes[Env1, Err]
                                                            ): Routes[Env1, Err] =
      routes.transform: handler =>
        Handler.scoped:
          Handler.fromFunctionZIO[Request]: request =>
            val origin = request.header(Header.Origin).map(_.renderedValue).getOrElse("unknown")
            ZIO.logInfo(
              s"""[Auth Middleware] Request Info:
                 |  Method: ${request.method}
                 |  Path: ${request.path}
                 |  Origin: $origin
                 |""".stripMargin
            ) *>
              (if (request.method == Method.OPTIONS) {
                // 直接放行，不做任何处理
                handler(request)
              } else {
                extractAndValidateToken(request).flatMap {
                  case (sub, payload) =>
                    for
                      ctx <- ZIO.service[JwtContent]
                      // 只有 Access token 才设置上下文
                      _ <- ZIO.when(payload.tokenType == Access) {
                        ctx.set(sub)
                      }
                      response <- handler(request)
                    yield response
                }
              })

    private def extractAndValidateToken(request: Request): ZIO[JwtService, Response, (String, Payload)] =
      extractToken(request).flatMap { token =>
        for
          jwtService <- ZIO.service[JwtService]
          claim <- jwtService
            .decode(token)
            .mapError(_ => Response.unauthorized("Invalid or expired token"))

          // 验证 token 是否过期
          now = java.time.Instant.now().getEpochSecond
          _ <- ZIO
            .fail(Response.unauthorized("Token expired"))
            .whenZIO(
              ZIO.fromOption(claim.expiration)
                .map(_ < now)
                .orElse(ZIO.succeed(false)) // 如果没有过期时间，则不检查
            )

          // 提取 subject
          sub <- ZIO
            .fromOption(claim.subject)
            .mapError(_ => Response.unauthorized("Missing subject in token"))

          // 解析 payload
          payload <- ZIO
            .fromEither(claim.content.fromJson[Payload])
            .mapError(e => Response.unauthorized(s"Invalid token payload: $e"))

          // 验证 token 类型
          _ <- ZIO
            .fail(Response.unauthorized("Invalid token type"))
            .unless(payload.tokenType == Access || payload.tokenType == Refresh)

        yield (sub, payload)
      }

    private def extractToken(request: Request): ZIO[Any, Response, String] =
      request.header(Header.Authorization) match
        case Some(Header.Authorization.Bearer(token)) =>
          ZIO.succeed(token.value.toArray.mkString)

        case _ =>
          // 尝试从 Cookie 中获取 token
          request.cookie("access_token") match
            case Some(cookie) =>
              ZIO.succeed(cookie.content)
            case None =>
              ZIO.fail(Response.unauthorized("Missing authorization token"))