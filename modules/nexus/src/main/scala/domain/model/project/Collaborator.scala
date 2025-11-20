package app.mosia.nexus
package domain.model.project

import domain.model.user.{UserId, UserRole}

import java.time.Instant

case class Collaborator(
  userId: UserId,
  role: UserRole,
  addedAt: Instant
)
