package app.mosia.nexus
package application.states

import domain.model.user.Provider

import zio.*
import zio.json.*

import java.time.Instant

case class OAuth2StateData(
  provider: Provider,
  redirectUri: String,
  platform: Option[String],
  createdAt: Instant
) derives JsonCodec
