package app.mosia.nexus
package domain.model.user

import zio.json.*
// Case class to parse GitHub user info
final case class GitHubUserInfo(
  id: Long,
  login: String,
  email: Option[String]
) derives JsonCodec
