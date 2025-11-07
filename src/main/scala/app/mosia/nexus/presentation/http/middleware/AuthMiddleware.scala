package app.mosia.nexus.presentation.http.middleware

import app.mosia.nexus.infra.auth.{JwtContent, JwtPayload, JwtService}
import zio.*
import zio.http.*
import zio.json.*

object AuthMiddleware:
  // 这是一个 ZIO HTTP 中间件，它会拦截请求
  // 如果请求包含有效的 JWT, 它会将 JWT 的内容 (JwtContent) 注入到 ZIO 环境中，然后交由被它包裹的路由处理器处理
  // 如果 JWT 无效或不存在, 它会直接返回 401 Unauthorized 错误
  val middleware: Middleware[JwtService & JwtContent] = new Middleware:
    override def apply[Env1 <: JwtService & JwtContent, Err](routes: Routes[Env1, Err]): Routes[Env1, Err] =
      routes.transform: handler =>
        Handler.scoped:
          Handler.fromFunctionZIO[Request]: request =>
            request.header(Header.Authorization) match
              case Some(Header.Authorization.Bearer(token)) =>
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
              case _ =>
                ZIO.fail(Response.unauthorized("Missing or invalid Authorization header"))
