package app.mosia.nexus
package domain.services.app

import domain.error.AppTask
import domain.model.verification.VerificationCodeType

/** 通知服务接口
  *
  * 统一的通知服务，整合邮件、短信和验证码功能
  */
trait NotificationService:

  /** 发送邮箱验证码
    *
    * @param email
    *   邮箱地址
    * @param codeType
    *   验证码类型
    * @return
    *   生成的验证码
    */
  def sendEmailVerificationCode(email: String, codeType: VerificationCodeType): AppTask[String]

  /** 发送短信验证码
    *
    * @param phone
    *   手机号
    * @param codeType
    *   验证码类型
    * @return
    *   生成的验证码
    */
  def sendSmsVerificationCode(phone: String, codeType: VerificationCodeType): AppTask[String]

  /** 验证邮箱验证码
    *
    * @param email
    *   邮箱地址
    * @param code
    *   验证码
    * @param codeType
    *   验证码类型
    * @return
    *   是否验证成功
    */
  def verifyEmailCode(email: String, code: String, token: String, codeType: VerificationCodeType): AppTask[Boolean]

  /** 验证短信验证码
    *
    * @param phone
    *   手机号
    * @param code
    *   验证码
    * @param codeType
    *   验证码类型
    * @return
    *   是否验证成功
    */
  def verifySmsCode(phone: String, code: String, token: String, codeType: VerificationCodeType): AppTask[Boolean]

  /** 发送欢迎邮件
    *
    * @param email
    *   邮箱地址
    * @param username
    *   用户名
    */
  def sendWelcomeEmail(email: String, username: String): AppTask[Unit]
