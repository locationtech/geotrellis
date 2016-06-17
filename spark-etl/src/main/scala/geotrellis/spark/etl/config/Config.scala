package geotrellis.spark.etl.config

import org.apache.spark.storage.StorageLevel

import scala.util.matching.Regex

case class Config(
  name: String,
  ingestType: IngestType,
  path: IngestPath,
  ingestOptions: IngestOptions,
  cache: Option[StorageLevel] = None
) {
  private def getParams(jbt: BackendType, p: String) = jbt match {
    case S3Type => {
      val Config.S3UrlRx(_, _, bucket, prefix) = p
      Map("bucket" -> bucket, "key" -> prefix)
    }
    case AccumuloType          => Map("table" -> p)
    case CassandraType         => {
      val List(keyspace, table) = p.split("\\.").toList
      Map("keyspace" -> keyspace, "table" -> table)
    }
    case HadoopType | FileType => Map("path" -> p)
  }

  def inputParams  = getParams(ingestType.input, path.input)
  def outputParams = getParams(ingestType.output, path.output)
}

object Config {
  val idRx = "[A-Z0-9]{20}"
  val keyRx = "[a-zA-Z0-9+/]+={0,2}"
  val slug = "[a-zA-Z0-9-]+"
  val S3UrlRx = new Regex(s"""s3://(?:($idRx):($keyRx)@)?($slug)/{0,1}(.*)""", "aws_id", "aws_key", "bucket", "prefix")
}
