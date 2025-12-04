package app.mosia.nexus
package domain.services.app

import domain.error.AppTask
import domain.model.user.*

trait UserService:
  def createUser(email: String, plainPassword: String, name: Option[String]): AppTask[User]
  def findByEmail(email: String): AppTask[Option[User]]
  def findById(id: String): AppTask[Option[User]]
  def authenticate(email: String, plainPassword: String): AppTask[Option[User]]
  def markEmailAsVerified(email: String): AppTask[Unit]
  def resetPassword(email: String, newPassword: String): AppTask[Unit]
  def createUserWithOAuth(
                           email: String,
                           username: Option[String],
                           provider: Provider,
                           providerUserId: String,
                           providerEmail: Option[String]
                         ): AppTask[User]
  def linkProvider(
                    userId: UserId,
                    provider: Provider,
                    providerUserId: String,
                    providerEmail: Option[String]
                  ): AppTask[OAuthProvider]