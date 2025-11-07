package app.mosia.nexus.domain.model.user

import app.mosia.nexus.domain.model.common.ValueObject
import io.getquill.MappedEncoding
import zio.json.JsonCodec

import scala.util.{Failure, Success, Try}

enum UserRole extends ValueObject derives JsonCodec:
  case Admin, Developer, Viewer

object UserRole:
  def fromString(str: String): Either[String, UserRole] =
    Try(UserRole.valueOf(str)).toEither.left.map(_ => s"Invalid role: $str")
