package app.mosia.nexus
package domain.error

/** ========== 400 Bad Request - 验证错误 ========== */
sealed trait ValidationError extends ClientError:
  def httpStatus: Int = 400

object ValidationError:
  /** 无效输入 */
  case class InvalidInput(
    field: String,
    reason: String,
    value: Option[String] = None,
    cause: Option[Throwable] = None,
    context: Map[String, String] = Map.empty
  ) extends ValidationError:
    val message: String = value match
      case Some(v) => s"Invalid value '$v' for field '$field': $reason"
      case None => s"Invalid input for field '$field': $reason"
    val errorCode = "INVALID_INPUT"

  /** 缺少必填字段 */
  case class MissingRequiredField(
    entityType: String,
    field: String,
    cause: Option[Throwable] = None,
    context: Map[String, String] = Map.empty
  ) extends ValidationError:
    val message   = s"Required field '$field' is missing for $entityType"
    val errorCode = "MISSING_REQUIRED_FIELD"

  /** 无效的字段值 */
  case class InvalidFieldValue(
    field: String,
    value: String,
    expectedFormat: String,
    cause: Option[Throwable] = None,
    context: Map[String, String] = Map.empty
  ) extends ValidationError:
    val message   = s"Invalid value '$value' for field '$field'. Expected: $expectedFormat"
    val errorCode = "INVALID_FIELD_VALUE"

  /** 业务规则违反 */
  case class BusinessRuleViolation(
    rule: String,
    details: String,
    cause: Option[Throwable] = None,
    context: Map[String, String] = Map.empty
  ) extends ValidationError:
    val message   = s"Business rule violation: $rule - $details"
    val errorCode = "BUSINESS_RULE_VIOLATION"

  /** 无效引用 - 引用了不存在的实体 */
  case class InvalidReference(
    entityType: String,
    field: String,
    referencedType: String,
    referencedId: String,
    cause: Option[Throwable] = None,
    context: Map[String, String] = Map.empty
  ) extends ValidationError:
    val message =
      s"$entityType references non-existent $referencedType '$referencedId' in field '$field'"
    val errorCode = "INVALID_REFERENCE"
