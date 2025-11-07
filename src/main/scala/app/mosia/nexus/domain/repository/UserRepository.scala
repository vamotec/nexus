package app.mosia.nexus.domain.repository

import app.mosia.nexus.domain.model.user.User
import app.mosia.nexus.infra.error.AppTask
import app.mosia.nexus.infra.persistence.postgres.entity.UserAuthRecord

import java.sql.SQLException
import java.util.UUID
import io.getquill.jdbczio.Quill
import zio.IO
import zio.ZLayer

trait UserRepository:
  def create(user: User, passwordHash: String): AppTask[User]
  def updatePassword(userId: UUID, newPasswordHash: String): AppTask[Unit]
  def findByEmail(email: String): AppTask[Option[User]]
  def findById(id: UUID): AppTask[Option[User]]
  def findPasswordHashByEmail(email: String): AppTask[Option[String]]
