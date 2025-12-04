package app.mosia.nexus
package codegen

case class DatabaseConfig(
  url: String,
  user: String,
  password: String,
  maxPoolSize: Int = 10
)
