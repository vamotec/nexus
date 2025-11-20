package app.mosia.nexus
package domain.event

import zio.json.*

import java.time.Instant

/** A domain event representing that a new user has registered.
  */
@jsonMemberNames(SnakeCase)
case class UserRegistered(
  userId: String,
  username: String,
  email: String,
  registeredAt: Instant
) derives JsonCodec
