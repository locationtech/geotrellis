package geotrellis.spark.etl.config

sealed trait BackendPath
case class S3Path(url: String, bucket: String, prefix: String) extends BackendPath {
  override def toString = url
}
case class AccumuloPath(table: String) extends BackendPath {
  override def toString = table
}
case class HBasePath(table: String) extends BackendPath {
  override def toString = table
}
case class CassandraPath(keyspace: String, table: String) extends BackendPath {
  override def toString = s"${keyspace}.${table}"
}
case class HadoopPath(path: String) extends BackendPath {
  override def toString = path
}
case class UserDefinedPath(path: String) extends BackendPath {
  override def toString = path
}
