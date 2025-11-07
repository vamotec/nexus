package app.mosia.nexus.domain.event

import zio.json.*

import java.time.Instant
import java.util.UUID

/** A domain event representing that a new user has registered.
  */
@jsonMemberNames(SnakeCase)
case class UserRegistered(
  userId: String,
  username: String,
  email: String,
  registeredAt: Instant
) derives JsonCodec
