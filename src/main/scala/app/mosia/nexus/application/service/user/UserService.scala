package app.mosia.nexus.application.service.user

import app.mosia.nexus.domain.model.user.{User, UserId}
import app.mosia.nexus.infra.error.AppTask
import zio.*

import java.sql.SQLException
import java.util.UUID

trait UserService:
  def createUser(email: String, plainPassword: String): AppTask[User]
  def findByEmail(email: String): AppTask[Option[User]]
  def findById(id: UserId): AppTask[Option[User]]
  def authenticate(email: String, plainPassword: String): AppTask[Option[User]]
