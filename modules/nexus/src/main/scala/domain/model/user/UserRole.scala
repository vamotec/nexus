package app.mosia.nexus
package domain.model.user

import domain.model.common.ValueObject

import app.mosia.nexus.domain.model.jwt.Permission
import zio.*
import zio.json.*

enum UserRole extends ValueObject derives JsonCodec:
  case Admin, Developer, Viewer

object UserRole:
  def fromString(s: String): UserRole = s.toLowerCase match
    case "admin" => UserRole.Admin
    case "developer" => UserRole.Developer
    case "viewer" => UserRole.Viewer
    case _ => throw new IllegalArgumentException(s"Unknown user role: $s")

  def toRoleStr(userRole: UserRole): String = userRole match
    case UserRole.Admin => "admin"
    case UserRole.Developer => "developer"
    case UserRole.Viewer => "viewer"

  def toPermission(userRole: UserRole): Permission = userRole match
    case UserRole.Admin => Permission.Admin
    case UserRole.Developer => Permission.Editor
    case UserRole.Viewer => Permission.Viewer
