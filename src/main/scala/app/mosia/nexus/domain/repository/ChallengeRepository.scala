package app.mosia.nexus.domain.repository

import app.mosia.nexus.domain.model.device.DeviceId
import app.mosia.nexus.domain.model.user.{ChallengeRow, UserId}
import app.mosia.nexus.infra.error.AppTask

trait ChallengeRepository:
  def createChallenge(
    userId: Option[UserId],
    deviceId: Option[DeviceId],
    purpose: String,
    ttlSeconds: Long
  ): AppTask[ChallengeRow]

  def consumeChallenge(
    challenge: String
  ): AppTask[Option[ChallengeRow]] // atomically mark consumed if valid and return row

  def findValidChallenge(challenge: String): AppTask[Option[ChallengeRow]]
