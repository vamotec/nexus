package app.mosia.nexus
package application.dto.response.user

import domain.model.user.User

import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

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
    avatar = user.avatar,
    role = user.role.toString,
    createdAt = user.createdAt.toString
  )
