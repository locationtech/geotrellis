package geotrellis.spark.etl.s3

import geotrellis.raster.Tile
import geotrellis.spark.etl._
import geotrellis.spark.ingest.{Tiler, IngestKey}
import geotrellis.spark.io.s3.S3InputFormat
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.mapreduce.Job
import org.apache.spark.SparkContext

import scala.reflect.ClassTag

abstract class S3Input[I: IngestKey, K: ClassTag](implicit tiler: Tiler[I, K, Tile]) extends IngestInputPlugin[I, K] {
  val name = "s3"
  val requiredKeys = Array("bucket", "key")

  def configuration(props: Map[String, String])(implicit sc: SparkContext): Configuration = {
    val job = Job.getInstance(sc.hadoopConfiguration, "S3 GeoTiff ETL")
    S3InputFormat.setBucket(job, props("bucket"))
    S3InputFormat.setPrefix(job, props("key"))
    if (props.contains("splitSize"))
      S3InputFormat.setMaxKeys(job, props("splitSize").toInt)
    job.getConfiguration
  }
}
