package app.mosia.nexus
package application.states

import java.time.Instant

import zio.json.*
import zio.*

case class OAuth2StateData(
  provider: String,
  platform: Option[String],
  createdAt: Instant
) derives JsonCodec
