package app.mosia.nexus
package domain.repository

import domain.error.AppTask
import domain.model.user.{Challenge, UserId}

trait ChallengeRepository:
  def createChallenge(
    userId: Option[UserId],
    deviceId: Option[String],
    purpose: String,
    ttlSeconds: Long
  ): AppTask[Challenge]

  def consumeChallenge(
    challenge: String
  ): AppTask[Option[Challenge]] // atomically mark consumed if valid and return row

  def findValidChallenge(challenge: String): AppTask[Option[Challenge]]
