package app.mosia.nexus.presentation.graphql.schema

import app.mosia.nexus.application.dto.response.session.{MetricsResponse, SessionResponse}
import app.mosia.nexus.domain.model.session.{NeuroSession, SessionId, SessionMetrics, SessionStatus}
import app.mosia.nexus.domain.model.simulation.{Simulation, SimulationId}
import app.mosia.nexus.domain.model.training.TrainingProgress
import caliban.schema.{ArgBuilder, Schema, SubscriptionSchema}
import zio.Task
import zio.json.JsonCodec

object SessionSchema:
  case class sessionsBySimulationArg(
    id: SimulationId,
    count: Int
  ) derives Schema.SemiAuto,
        ArgBuilder

  case class MySessionsArg(
    count: Int,
    status: SessionStatus
  ) derives Schema.SemiAuto,
        ArgBuilder

  case class Queries(
    session: SessionId => Task[Option[SessionResponse]],
    sessionsBySimulation: sessionsBySimulationArg => Task[List[SessionResponse]],
    mySessions: MySessionsArg => Task[List[SessionResponse]]
  ) derives Schema.SemiAuto

  case class Mutations(
    create: SimulationId => Task[SessionResponse],
    start: SessionId => Task[SessionResponse],
    pause: SessionId => Task[SessionResponse],
    resume: SessionId => Task[SessionResponse],
    stop: SessionId => Task[SessionResponse]
  ) derives Schema.SemiAuto

//  case class Subscriptions(
//    sessionMetrics: SessionId => Task[MetricsResponse],
//    sessionStatus: SessionId => Task[SessionStatus]
//  ) derives Schema.SemiAuto
