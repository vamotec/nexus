package app.mosia.nexus
package domain.services.infra

import domain.error.AppTask
import domain.model.verification.{VerificationCode, VerificationCodeType}

/** 验证码服务接口
  *
  * 负责验证码的生成、存储和校验
  */
trait VerificationCodeService:

  /** 生成并存储验证码
    *
    * 包含限流检查：
    * - 60秒内不能重复发送
    * - 每天最多发送5次
    *
    * @param target
    *   发送目标（邮箱或手机号）
    * @param codeType
    *   验证码类型
    * @param validMinutes
    *   有效时长（分钟，默认5分钟）
    * @return
    *   生成的验证码
    */
  def generate(target: String, codeType: VerificationCodeType, validMinutes: Int = 5): AppTask[VerificationCode]

  /** 验证并消费验证码
    *
    * 验证成功后会删除验证码（一次性使用）
    *
    * @param target
    *   发送目标
    * @param code
    *   验证码
    * @param codeType
    *   验证码类型
    * @return
    *   是否验证成功
    */
  def verify(target: String, code: String, token: String, codeType: VerificationCodeType): AppTask[Boolean]

  /** 获取验证码（不消费）
    *
    * @param target
    *   发送目标
    * @param codeType
    *   验证码类型
    * @return
    *   验证码（如果存在）
    */
  def get(target: String, codeType: VerificationCodeType): AppTask[Option[VerificationCode]]

  /** 删除验证码
    *
    * @param target
    *   发送目标
    * @param codeType
    *   验证码类型
    */
  def delete(target: String, codeType: VerificationCodeType): AppTask[Unit]
