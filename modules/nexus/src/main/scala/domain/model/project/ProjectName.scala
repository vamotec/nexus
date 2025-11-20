package app.mosia.nexus
package domain.model.project

import zio.json.*
import zio.*

opaque type ProjectName = String

object ProjectName:

  def from(value: String): Either[String, ProjectName] =
    val trimmed = value.trim.toLowerCase

    if (trimmed.isEmpty)
      Left("Project name cannot be empty")
    else if (trimmed.length < 3)
      Left("Project name must be at least 3 characters")
    else if (trimmed.length > 50)
      Left("Project name cannot exceed 50 characters")
    else if (!trimmed.matches("^[a-zA-Z0-9][a-zA-Z0-9_-]*$"))
      Left(
        "Project name must start with a letter or number, and can only contain letters, numbers, hyphens and underscores"
      )
    else if (trimmed.matches(".*[-_]{2,}.*"))
      Left("Project name cannot contain consecutive hyphens or underscores")
    else
      Right(trimmed)

  def fromZIO(value: String): IO[String, ProjectName] =
    ZIO.fromEither(from(value))

  def unsafe(value: String): ProjectName =
    from(value).fold(
      error => throw new IllegalArgumentException(error),
      identity
    )

  extension (name: ProjectName) def value: String = name

  // JSON 编解码
  given JsonEncoder[ProjectName] = JsonEncoder[String].contramap(identity)
  given JsonDecoder[ProjectName] = JsonDecoder[String].mapOrFail(str => from(str))
