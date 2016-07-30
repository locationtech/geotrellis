package geotrellis.spark.etl.config

sealed trait BackendType {
  val name: String

  override def toString = name
}

sealed trait BackendInputType extends BackendType

case object AccumuloType extends BackendType {
  val name = "accumulo"
}

case object HBaseType extends BackendType {
  val name = "hbase"
}

case object CassandraType extends BackendType {
  val name = "cassandra"
}

case object S3Type extends BackendInputType {
  val name = "s3"
}

case object HadoopType extends BackendInputType {
  val name = "hadoop"
}

case object FileType extends BackendType {
  val name = "file"
}

case class UserDefinedBackendType(name: String) extends BackendType

case class UserDefinedBackendInputType(name: String) extends BackendInputType

object BackendType {
  def fromString(str: String) = str match {
    case AccumuloType.name  => AccumuloType
    case CassandraType.name => CassandraType
    case HBaseType.name     => HBaseType
    case S3Type.name        => S3Type
    case HadoopType.name    => HadoopType
    case FileType.name      => FileType
    case s                  => UserDefinedBackendType(s)
  }
}

object BackendInputType {
  def fromString(str: String) = str match {
    case S3Type.name     => S3Type
    case HadoopType.name => HadoopType
    case s               => UserDefinedBackendInputType(s)
  }
}

