package app.mosia.nexus.domain.repository

import app.mosia.nexus.domain.model.user.RefreshToken
import app.mosia.nexus.infra.error.AppTask

trait RefreshTokenRepository:
  def save(refreshToken: RefreshToken): AppTask[Unit]
  def findByToken(token: String): AppTask[Option[RefreshToken]]
  def markAsRevoked(token: String): AppTask[Unit]
  def delete(token: String): AppTask[Unit]
