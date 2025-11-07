package app.mosia.nexus.domain.model.common

import java.util.UUID

/** 实体 ID 的类型类 */
trait EntityId[A]:
  def value: UUID

object EntityId:
  def fromString[A](str: String)(constructor: UUID => A): Either[String, A] =
    try Right(constructor(UUID.fromString(str)))
    catch
      case _: IllegalArgumentException =>
        Left(s"Invalid UUID format: $str")
