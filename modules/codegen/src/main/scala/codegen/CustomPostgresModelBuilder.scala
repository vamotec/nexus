package app.mosia.nexus
package codegen

import slick.jdbc.PostgresProfile
import slick.jdbc.meta.MTable

import scala.concurrent.ExecutionContext
import scala.reflect.ClassTag

// 自定义代码生成器
class CustomPostgresModelBuilder(mTables: Seq[MTable], ignoreInvalidDefaults: Boolean)(implicit ec: ExecutionContext)
    extends PostgresProfile.ModelBuilder(mTables, ignoreInvalidDefaults):

  override def jdbcTypeToScala(jdbcType: Int, typeName: String = ""): ClassTag[?] =
    import java.sql.Types.*
    import scala.reflect.classTag
    // see TABLE B-1 of JSR-000221 JBDCTM API Specification 4.1 Maintenance Release
    // Mapping to corresponding Scala types where applicable
    jdbcType match
      case CHAR | VARCHAR | LONGVARCHAR | NCHAR | NVARCHAR | LONGNVARCHAR => classTag[String]
      case NUMERIC | DECIMAL => classTag[BigDecimal]
      case BIT | BOOLEAN => classTag[Boolean]
      case TINYINT => classTag[Byte]
      case SMALLINT => classTag[Short]
      case INTEGER => classTag[Int]
      case BIGINT => classTag[Long]
      case REAL => classTag[Float]
      case FLOAT | DOUBLE => classTag[Double]
      case BINARY | VARBINARY | LONGVARBINARY | BLOB => classTag[java.sql.Blob]
      case DATE => classTag[java.sql.Date]
      case TIME => classTag[java.sql.Time]
      case TIMESTAMP => classTag[java.sql.Timestamp]
      case CLOB => classTag[java.sql.Clob]
      // case ARRAY => classTag[java.sql.Array]
      // case STRUCT => classTag[java.sql.Struct]
      // case REF => classTag[java.sql.Ref]
      // case DATALINK => classTag[java.net.URL]
      // case ROWID => classTag[java.sql.RowId]
      // case NCLOB => classTag[java.sql.NClob]
      // case SQLXML => classTag[java.sql.SQLXML]
      case NULL => ClassTag.Null
      case OTHER => classTag[java.sql.Blob]
      case ARRAY =>
        typeName.toLowerCase match
          case "_text" => classTag[java.sql.Clob]
          case _ => classTag[java.sql.Array]
      case DISTINCT =>
        logger.warn(
          s"Found jdbc type DISTINCT. Assuming Blob. This may be wrong. You can override ModelBuilder#Table#Column#tpe to fix this."
        );
        classTag[java.sql.Blob] // FIXME
      case t =>
        logger.warn(
          s"Found unknown jdbc type $t. Assuming String. This may be wrong. You can override ModelBuilder#Table#Column#tpe to fix this."
        );
        classTag[String] // FIXME
