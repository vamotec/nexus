package app.mosia.nexus
package domain.repository

import domain.error.AppTask
import domain.model.user.Authenticator

import java.time.Instant
import java.util.UUID

/**
 * 用于手机生物信息登陆方式
 */
trait AuthenticatorRepository:
  def findByDeviceIdAndKeyId(deviceId: String, keyId: String): AppTask[Option[Authenticator]]

  def updateSignCount(id: UUID, newCount: Option[Long]): AppTask[Unit]

  def setLastUsed(id: UUID, when: Instant): AppTask[Unit]
