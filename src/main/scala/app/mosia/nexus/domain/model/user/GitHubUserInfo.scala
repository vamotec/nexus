package app.mosia.nexus.domain.model.user

import zio.json.JsonCodec

// Case class to parse GitHub user info
final case class GitHubUserInfo(
  id: Long,
  login: String,
  email: Option[String]
) derives JsonCodec
