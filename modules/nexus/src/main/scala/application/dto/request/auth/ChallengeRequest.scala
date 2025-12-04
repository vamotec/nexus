package app.mosia.nexus
package application.dto.request.auth

import zio.*
import zio.json.*

case class ChallengeRequest(userId: Option[String], deviceId: Option[String]) derives JsonCodec
