package app.mosia.nexus.application.dto.response.user

import app.mosia.nexus.domain.model.user.User
import sttp.tapir.Schema
import zio.json.{jsonField, JsonCodec}

import java.time.Instant
import java.util.UUID

case class UserResponse(
  id: String,
  username: String,
  email: String,
  avatar: Option[String],
  role: String, // "ADMIN" | "USER"
  @jsonField("created_at") createdAt: String
) derives JsonCodec,
      Schema

object UserResponse:
  def fromDomain(user: User): UserResponse = UserResponse(
    id = user.id.value.toString,
    username = user.name,
    email = user.email,
    avatar = Some(user.avatar),
    role = user.role.toString,
    createdAt = user.createdAt.toString
  )
