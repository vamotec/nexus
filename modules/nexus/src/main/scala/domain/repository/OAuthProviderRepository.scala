package app.mosia.nexus
package domain.repository

import domain.error.AppTask
import domain.model.user.{OAuthProvider, Provider, UserId}

import java.time.Instant

trait OAuthProviderRepository:
  def findByProviderAndProviderId(provider: Provider, providerId: String): AppTask[Option[OAuthProvider]]

  def create(oauthProvider: OAuthProvider): AppTask[OAuthProvider]

  def updateLastUsed(provider: Provider, providerId: String, timestamp: Instant): AppTask[Unit]

  def findByUserId(userId: UserId): AppTask[List[OAuthProvider]]
