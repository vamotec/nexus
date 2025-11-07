package app.mosia.nexus.infra.config

final case class OAuth2Config(clients: Map[String, OAuth2ClientConfig])
