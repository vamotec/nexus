package app.mosia.nexus
package domain.services.app

import domain.error.AppTask
import domain.model.user.{User, UserId}

import java.sql.SQLException
import java.util.UUID

trait UserService:
  def createUser(email: String, plainPassword: String): AppTask[User]
  def findByEmail(email: String): AppTask[Option[User]]
  def findById(id: UserId): AppTask[Option[User]]
  def authenticate(email: String, plainPassword: String): AppTask[Option[User]]
