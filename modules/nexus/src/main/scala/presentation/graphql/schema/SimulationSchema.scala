package app.mosia.nexus
package presentation.graphql.schema

import domain.services.infra.JwtContent
import application.dto.request.simulation.{CreateSimulationRequest, UpdateSimulationRequest}
import application.dto.response.simulation.{SimulationListResponse, SimulationResponse}
import domain.error.CalTask
import domain.model.project.ProjectId
import domain.model.simulation.{Simulation, SimulationId}
import domain.model.user.UserId

import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

object SimulationSchema:
  case class SimulationQueries(
    simulation: String => CalTask[Option[SimulationResponse]],
    simulationsByProject: String => CalTask[List[SimulationListResponse]]
  ) derives Cs.SemiAuto

  case class UpdateSimulationArgs(
    simulationId: String,
    request: UpdateSimulationRequest
  ) derives Cs.SemiAuto,
        ArgBuilder

  case class CloneSimulationArgs(
    simulationId: String,
    name: String
  ) derives Cs.SemiAuto,
        ArgBuilder

  case class SimulationMutations(
    createSimulation: CreateSimulationRequest => CalTask[SimulationResponse],
    updateSimulation: UpdateSimulationArgs => CalTask[SimulationResponse],
    deleteSimulation: String => CalTask[Boolean],
    cloneSimulation: CloneSimulationArgs => CalTask[SimulationResponse]
  ) derives Cs.SemiAuto
