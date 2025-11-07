package app.mosia.nexus.infra.persistence.postgres.entity

import java.util.UUID

case class UserAuthRecord(id: UUID, email: String, name: String, passwordHash: String)
