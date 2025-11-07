package app.mosia.nexus.infra.persistence

import app.mosia.nexus.domain.model.user.UserRole
import app.mosia.nexus.infra.error.InternalError.InvalidUserRole
import io.getquill.MappedEncoding

trait QuillUserCodecs:
  given encodeUserRole: MappedEncoding[UserRole, String] =
    MappedEncoding[UserRole, String](_.toString)

  given decodeUserRole: MappedEncoding[String, UserRole] =
    MappedEncoding { str =>
      UserRole.fromString(str) match
        case Right(role) => role
        case Left(error) => throw InvalidUserRole(error)
    }
