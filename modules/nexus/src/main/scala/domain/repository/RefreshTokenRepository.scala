package app.mosia.nexus
package domain.repository

import domain.error.AppTask
import domain.model.user.RefreshToken

trait RefreshTokenRepository:
  def save(refreshToken: RefreshToken): AppTask[Unit]
  def findByToken(token: String): AppTask[Option[RefreshToken]]
  def markAsRevoked(token: String): AppTask[Unit]
  def delete(token: String): AppTask[Unit]
