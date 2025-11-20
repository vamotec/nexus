package app.mosia.nexus
package domain.config.auth

final case class OAuth2Config(clients: Map[String, OAuth2ClientConfig])
