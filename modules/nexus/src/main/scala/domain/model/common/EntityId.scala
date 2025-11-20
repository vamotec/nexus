package app.mosia.nexus
package domain.model.common

import domain.error.*

import zio.*
import java.util.UUID

/** 实体 ID 的类型类 */
trait EntityId[A]:
  def value: UUID

object EntityId:
  def fromString[A](str: String)(implicit ev: UUID => A): IO[AppError, A] =
    ZIO
      .attempt(ev(UUID.fromString(str)))
      .mapError { case _: IllegalArgumentException =>
        InvalidFieldValue("id", str, "UUID")
      }

  // 可选：安全的创建方法
  def fromStringSafe[A](str: String)(implicit ev: UUID => A): Option[A] =
    try Some(ev(UUID.fromString(str)))
    catch case _: IllegalArgumentException => None

  // 可选：验证方法
  def isValidUUID(str: String): Boolean =
    str.matches("""^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$""")
