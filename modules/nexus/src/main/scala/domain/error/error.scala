package app.mosia.nexus
package domain

import caliban.CalibanError
import zio.*

package object error:
  // 统一导出所有错误类型
  export AppError.*
  export AuthenticationError.*
  export AuthorizationError.*
  export ClientError.*
  export ConflictError.*
  export DatabaseError.*
  export ExternalServiceError.*
  export InternalError.*
  export ValidationError.*

  // 导出工具类
//  export ErrorResponse.*

  // 常用类型别名
  type AppTask[+A] = ZIO[Any, AppError, A]
  type CalTask[+A] = ZIO[Any, CalibanError, A]

  /** Extension 方法 */
  extension (error: Throwable)
    def toAppError: AppError                    = AppError.toAppErrorMapper(error)
    def toAppError(operation: String): AppError = AppError.toAppErrorMapper(error, operation)
