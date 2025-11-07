package app.mosia.nexus.presentation.graphql.resolver

import app.mosia.nexus.application.service.user.UserService
import app.mosia.nexus.presentation.graphql.schema.UserSchema.*

object UserResolver:
  def queries(service: UserService) = Queries()

  def mutations(service: UserService) = Mutations()
