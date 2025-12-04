package app.mosia.nexus
package domain.model.verification

import sttp.tapir.Schema
import zio.json.JsonCodec

/** 验证码类型 */
enum VerificationCodeType derives JsonCodec, Schema:
  case Email      // 邮箱验证码
  case Sms        // 短信验证码
  case Login      // 登录验证码
  case Register   // 注册验证码
  case ResetPassword  // 重置密码验证码