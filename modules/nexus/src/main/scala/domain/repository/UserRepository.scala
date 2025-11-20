package app.mosia.nexus
package domain.repository

import domain.error.AppTask
import domain.model.user.User

import java.sql.SQLException
import java.util.UUID
import io.getquill.jdbczio.Quill

trait UserRepository:
  def create(user: User, passwordHash: String): AppTask[Unit]
  def updatePassword(userId: UUID, newPasswordHash: String): AppTask[Unit]
  def findByEmail(email: String): AppTask[Option[User]]
  def findById(id: UUID): AppTask[Option[User]]
  def findPasswordHashByEmail(email: String): AppTask[Option[String]]
