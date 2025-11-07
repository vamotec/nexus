package app.mosia.nexus.application.dto.request.auth

import zio.json.JsonCodec

case class ChallengeRequest(userId: Option[String], deviceId: Option[String]) derives JsonCodec
