package app.mosia.nexus
package application.dto.response.auth

import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

@jsonMemberNames(SnakeCase)
case class ChallengeResponse(challenge: String, expiresAt: Long) derives JsonCodec
