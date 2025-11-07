package app.mosia.nexus.infra.persistence.postgres.entity

import app.mosia.nexus.domain.model.user.UserRole
import zio.json.JsonCodec

import java.time.Instant
import java.util.UUID

// 数据库实体
case class UserEntity(
  id: UUID,
  email: String,
  name: String,
  avatar: String,
  passwordHash: String,
  organization: Option[String],
  role: UserRole, // 或 UserRole，取决于你的编解码器
  isActive: Boolean,
  emailVerified: Boolean,
  createdAt: Instant,
  updatedAt: Instant,
  lastLoginAt: Option[Instant]
)
