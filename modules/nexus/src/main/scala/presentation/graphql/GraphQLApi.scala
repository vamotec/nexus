package app.mosia.nexus
package presentation.graphql

import domain.services.app.*
import domain.services.infra.JwtContent
import presentation.graphql.resolver.{ProjectResolver, SessionResolver, SimulationResolver, TrainingResolver}
import presentation.graphql.schema.*

import caliban.quick.*
import caliban.schema.Schema as Cs
import caliban.{CalibanError, GraphiQLHandler, RootResolver, graphQL}
import zio.*
import zio.http.*

object GraphQLApi:
  private final case class Queries(
    session: SessionSchema.SessionQueries,
    simulation: SimulationSchema.SimulationQueries,
    project: ProjectSchema.ProjectQueries,
    training: TrainingSchema.TrainingQueries
  ) derives Cs.SemiAuto

  private final case class Mutations(
    session: SessionSchema.SessionMutations,
    simulation: SimulationSchema.SimulationMutations,
    project: ProjectSchema.ProjectMutations,
    training: TrainingSchema.TrainingMutations
  ) derives Cs.SemiAuto

  private final case class Subscriptions(
    session: SessionSchema.SessionSubscriptions,
    project: ProjectSchema.ProjectSubscriptions,
    training: TrainingSchema.TrainingSubscriptions
  ) derives Cs.SemiAuto

  def make: ZIO[
    JwtContent & ProjectService & SimulationService & SessionService & TrainingService & UserService,
    CalibanError.ValidationError,
    Routes[Any, Nothing]
  ] =
    for
      // 获取模块服务
      userService <- ZIO.service[UserService]
      sessionService <- ZIO.service[SessionService]
      simulationService <- ZIO.service[SimulationService]
      projectService <- ZIO.service[ProjectService]
      trainingService <- ZIO.service[TrainingService]
      jwtContent <- ZIO.service[JwtContent]

      // 组合查询
      queries = Queries(
        session = SessionResolver.queries(sessionService),
        simulation = SimulationResolver.queries(simulationService, jwtContent),
        project = ProjectResolver.queries(projectService, simulationService, jwtContent),
        training = TrainingResolver.queries(trainingService, jwtContent)
      )

      // 组合变更
      mutations = Mutations(
        session = SessionResolver.mutations(sessionService),
        simulation = SimulationResolver.mutations(simulationService, jwtContent),
        project = ProjectResolver.mutations(projectService, simulationService, jwtContent),
        training = TrainingResolver.mutations(trainingService, jwtContent)
      )

      // 组合订阅
      subscriptions = Subscriptions(
        session = SessionResolver.subscriptions(sessionService),
        project = ProjectResolver.subscriptions(),
        training = TrainingResolver.subscriptions(trainingService)
      )

      // 创建 GraphQL API（包含查询、变更和订阅）
      api = graphQL(RootResolver(queries, mutations, subscriptions))
      handlers <- api.handlers
    yield Routes(
      Method.POST / "api" / "graphql" -> handlers.api,
      Method.ANY / "ws" / "graphql" -> handlers.webSocket,
      Method.GET / "graphiql" -> GraphiQLHandler.handler("/api/graphql", Some("/ws/graphql"))
    )
