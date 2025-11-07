package app.mosia.nexus.presentation.graphql.schema

import caliban.schema.Schema

object UserSchema:
  case class Queries() derives Schema.SemiAuto

  case class Mutations() derives Schema.SemiAuto
