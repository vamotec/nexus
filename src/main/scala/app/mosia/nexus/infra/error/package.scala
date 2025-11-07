package app.mosia.nexus.infra

import zio.ZIO

import java.sql.SQLException
import javax.sql.DataSource

package object error:
  // 统一导出所有错误类型
  export AppError.*
  export ValidationError.*
  export AuthError.*
  export DomainError.*
  export ExternalServiceError.*
  export InternalError.*

  // 导出工具类
  export ErrorMapper.*
  export ErrorResponse.*
  export ZIOErrorHandling.*

  // 常用类型别名
  type AppTask[+A] = ZIO[Any, AppError, A]
  type ZSQL[+A]    = ZIO[DataSource, SQLException, A]
