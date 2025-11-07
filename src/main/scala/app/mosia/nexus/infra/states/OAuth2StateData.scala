package app.mosia.nexus.infra.states

import zio.json.JsonCodec

import java.time.Instant

case class OAuth2StateData(
  provider: String,
  platform: Option[String],
  createdAt: Instant
) derives JsonCodec
