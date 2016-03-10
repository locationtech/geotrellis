package geotrellis.spark.io

/** Base trait for layer headers that store location information for a saved layer */
trait LayerHeader {
  def format: StorageFormat
  def keyClass: String
  def valueClass: String
}

sealed class StorageFormat(val name: String)

object StorageFormat {
  case object S3 extends StorageFormat("s3")
  case object HDFS extends StorageFormat("hdfs")
  case object Accumulo extends StorageFormat("accumulo")
  case object File extends StorageFormat("file")

  def fromString(name: String) = name match {
    case "s3" => S3
    case "hdfs" => HDFS
    case "accumulo" => Accumulo
    case "file" => File
  }
}
