package app.mosia.nexus.infra.config

case class DbSource(
  defaultdb: DbConfig,
  timescaledb: DbConfig
)
