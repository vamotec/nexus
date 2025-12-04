package app.mosia.nexus
package migration

case class DatabaseConfig(
  url: String,
  user: String,
  password: String,
  maxPoolSize: Int = 10
)

case class MigrationConfig(
  postgres: DatabaseConfig,
  locations: MigrationLocations,
  codegen: CodegenConfig,
  validateOnly: Boolean = false,
  cleanDisabled: Boolean = true,
  baselineOnMigrate: Boolean = true
)

case class MigrationLocations(
  postgres: String = "classpath:db/migration/postgres"
)

case class CodegenConfig(
  enabled: Boolean = true,
  postgresPackage: String = "infrastructure.persistence.postgres.rows"
)
