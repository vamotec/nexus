package app.mosia.nexus.presentation.graphql.schema

import app.mosia.nexus.application.dto.request.simulation.{CreateSimulationRequest, UpdateSimulationRequest}
import app.mosia.nexus.application.dto.response.simulation.{SimulationListResponse, SimulationResponse}
import app.mosia.nexus.domain.model.project.ProjectId
import app.mosia.nexus.domain.model.simulation.{Simulation, SimulationId}
import caliban.schema.{ArgBuilder, Schema}
import zio.Task

object SimulationSchema:
  case class CloneSimulationArg(id: SimulationId, name: String) derives Schema.SemiAuto, ArgBuilder

  case class Queries(
    simulation: SimulationId => Task[Option[SimulationResponse]],
    simulationsByProject: ProjectId => Task[List[SimulationListResponse]]
  ) derives Schema.SemiAuto

  case class Mutations(
    create: CreateSimulationRequest => Task[SimulationResponse],
    update: UpdateSimulationRequest => Task[SimulationResponse],
    delete: SimulationId => Task[Boolean],
    cloneSimulation: CloneSimulationArg => Task[SimulationResponse]
  ) derives Schema.SemiAuto
