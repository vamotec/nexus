package app.mosia.nexus
package application.dto.request.auth

import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

case class ChallengeRequest(userId: Option[String], deviceId: Option[String]) derives JsonCodec
