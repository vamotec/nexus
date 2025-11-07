package app.mosia.nexus.presentation.graphql

import app.mosia.nexus.application.service.session.SessionService
import app.mosia.nexus.application.service.user.UserService
import app.mosia.nexus.presentation.graphql.resolver.{SessionResolver, UserResolver}
import app.mosia.nexus.presentation.graphql.schema.*
import caliban.{graphQL, CalibanError, GraphiQLHandler, RootResolver}
import caliban.quick.*
import caliban.schema.Schema
import zio.ZIO
import zio.http.*

object GraphQLApi:
  final case class Queries(
    user: UserSchema.Queries,
    session: SessionSchema.Queries
  ) derives Schema.SemiAuto

  final case class Mutations(
    user: UserSchema.Mutations,
    session: SessionSchema.Mutations
  ) derives Schema.SemiAuto

  def make: ZIO[SessionService & UserService, CalibanError.ValidationError, Routes[Any, Nothing]] =
    for
      // 获取模块服务
      userService <- ZIO.service[UserService]
      sessionServie <- ZIO.service[SessionService]

      // 组合
      queries = Queries(
        UserResolver.queries(userService),
        SessionResolver.queries(sessionServie)
      )
      mutations = Mutations(
        UserResolver.mutations(userService),
        SessionResolver.mutations(sessionServie)
      )
      api = graphQL(RootResolver(queries, mutations))
      handlers <- api.handlers
    yield Routes(
      Method.POST / "api" / "graphql" -> handlers.api,
      Method.ANY / "ws" / "graphql" -> handlers.webSocket,
      Method.GET / "graphiql" -> GraphiQLHandler.handler("/api/graphql", Some("/ws/graphql"))
    )
