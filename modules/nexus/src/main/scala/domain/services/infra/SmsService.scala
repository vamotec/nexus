package app.mosia.nexus
package domain.services.infra

import domain.error.AppTask

/** 短信发送服务接口 */
trait SmsService:

  /** 发送验证码短信
    *
    * @param phone
    *   手机号
    * @param code
    *   验证码
    * @return
    *   Task[Unit]
    */
  def sendVerificationCode(phone: String, code: String): AppTask[Unit]

  /** 发送通知短信
    *
    * @param phone
    *   手机号
    * @param message
    *   短信内容
    * @return
    *   Task[Unit]
    */
  def sendNotification(phone: String, message: String): AppTask[Unit]
