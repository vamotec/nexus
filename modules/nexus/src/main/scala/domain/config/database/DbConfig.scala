package app.mosia.nexus
package domain.config.database

case class DbConfig(
                     dataSourceClassName: String,
                     dataSource: DataSourceConfig,
                     connectionTimeout: Long,
                     maximumPoolSize: Int,
                     idleTimeout: Long,
                     maxLifetime: Long
                   )
