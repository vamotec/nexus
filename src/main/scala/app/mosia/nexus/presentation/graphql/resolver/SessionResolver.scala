package app.mosia.nexus.presentation.graphql.resolver

import app.mosia.nexus.application.service.session.SessionService
import app.mosia.nexus.presentation.graphql.schema.SessionSchema.*

object SessionResolver:
  def queries(service: SessionService) = Queries(
    session = ???,
    sessionsBySimulation = ???,
    mySessions = ???
  )

  def mutations(service: SessionService) = Mutations(
    create = ???,
    start = ???,
    pause = ???,
    resume = ???,
    stop = ???
  )
