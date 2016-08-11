package geotrellis.spark.etl.s3

import geotrellis.spark.etl._
import geotrellis.spark.io.s3.S3InputFormat
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.mapreduce.Job
import org.apache.spark.SparkContext

abstract class S3Input[I, V] extends InputPlugin[I, V] {
  val name = "s3"

  def configuration(props: Map[String, String])(implicit sc: SparkContext): Configuration = {
    val job = Job.getInstance(sc.hadoopConfiguration, "S3 GeoTiff ETL")
    S3InputFormat.setBucket(job, props("bucket"))
    S3InputFormat.setPrefix(job, props("key"))
    if (props.contains("partitionCount"))
      S3InputFormat.setPartitionCount(job, props("partitionCount").toInt)
    if (props.contains("partitionBytes"))
      S3InputFormat.setPartitionBytes(job, props("partitionBytes").toInt)
    job.getConfiguration
  }
}
