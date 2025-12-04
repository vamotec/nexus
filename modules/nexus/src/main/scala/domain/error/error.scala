package app.mosia.nexus
package domain

import zio.*

package object error:
  // 统一导出所有错误类型
  export AppError.*
  export AuthenticationError.*
  export AuthorizationError.PermissionDenied
  export AuthorizationError.InsufficientPermissions
  export ClientError.*
  export ConflictError.{ConcurrentModification, DuplicateEntity, StateConflict, AlreadyExists}
  export DatabaseError.{General, ConnectionError, QueryTimeout, Deadlock}
  export ExternalServiceError.*
  export InternalError.{UnexpectedError, ConfigurationError, InvalidSystemState, ResourceExhausted}
  export ValidationError.{InvalidInput, MissingRequiredField, InvalidFieldValue, BusinessRuleViolation, InvalidReference, SerializationError}

  // 导出工具类
  export ErrorResponse.*

  // 常用类型别名
  type AppTask[+A] = ZIO[Any, AppError, A]

  /** Extension 方法 */
  extension (error: Throwable)
    def toAppError: AppError                    = AppError.toAppErrorMapper(error)
    def toAppError(operation: String): AppError = AppError.toAppErrorMapper(error, operation)
