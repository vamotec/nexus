package app.mosia.nexus.application.dto.response.auth

import zio.json.JsonCodec

case class ChallengeResponse(challenge: String, expiresAt: Long) derives JsonCodec
