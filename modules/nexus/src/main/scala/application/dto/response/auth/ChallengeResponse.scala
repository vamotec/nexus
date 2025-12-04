package app.mosia.nexus
package application.dto.response.auth

import zio.*
import zio.json.*

@jsonMemberNames(SnakeCase)
case class ChallengeResponse(challenge: String, expiresAt: Long) derives JsonCodec
