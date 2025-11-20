package app.mosia.nexus
package domain.config.database

case class DbSource(
  default: DbConfig,
  timescale: DbConfig
)
