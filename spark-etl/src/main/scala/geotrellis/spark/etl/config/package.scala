package geotrellis.spark.etl

import scala.util.matching.Regex

package object config {
  val idRx = "[A-Z0-9]{20}"
  val keyRx = "[a-zA-Z0-9+/]+={0,2}"
  val slug = "[a-zA-Z0-9-]+"
  val S3UrlRx = new Regex(s"""s3://(?:($idRx):($keyRx)@)?($slug)/{0,1}(.*)""", "aws_id", "aws_key", "bucket", "prefix")

  def getParams(jbt: BackendType, p: String) = jbt match {
    case S3Type => {
      val S3UrlRx(_, _, bucket, prefix) = p
      Map("bucket" -> bucket, "key" -> prefix)
    }
    case AccumuloType          => Map("table" -> p)
    case CassandraType         => {
      val List(keyspace, table) = p.split("\\.").toList
      Map("keyspace" -> keyspace, "table" -> table)
    }
    case HadoopType | FileType          => Map("path" -> p)
    case UserDefinedBackendType(s)      => Map("input" -> s)
    case UserDefinedBackendInputType(s) => Map("input" -> s)
  }
}
