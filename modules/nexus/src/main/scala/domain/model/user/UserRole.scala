package app.mosia.nexus
package domain.model.user

import domain.error.*
import domain.model.common.ValueObject

import io.getquill.MappedEncoding
import scala.util.Try
import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

enum UserRole extends ValueObject derives JsonCodec:
  case Admin, Developer, Viewer

object UserRole:
  def fromString(s: String): UserRole = s.toLowerCase match
    case "admin" => UserRole.Admin
    case "developer" => UserRole.Developer
    case "viewer" => UserRole.Viewer
    case _ => throw new IllegalArgumentException(s"Unknown user role: $s")

  def toString(userRole: UserRole): String = userRole match
    case UserRole.Admin => "box"
    case UserRole.Developer => "sphere"
    case UserRole.Viewer => "cylinder"
