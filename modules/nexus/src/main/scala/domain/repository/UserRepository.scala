package app.mosia.nexus
package domain.repository

import domain.error.AppTask
import domain.model.user.{User, UserId}

trait UserRepository:
  def create(user: User, passwordHash: String): AppTask[Unit]
  def updatePassword(userId: UserId, newPasswordHash: String): AppTask[Unit]
  def updatePasswordByEmail(email: String, newPasswordHash: String): AppTask[Unit]
  def markEmailAsVerified(email: String): AppTask[Unit]
  def findByEmail(email: String): AppTask[Option[User]]
  def findById(id: UserId): AppTask[Option[User]]
  def findPasswordHashByEmail(email: String): AppTask[Option[String]]
