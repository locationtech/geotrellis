package geotrellis.spark.io.s3

import geotrellis.spark._
import geotrellis.spark.io._

import org.apache.spark.SparkContext
import com.amazonaws.auth.AWSCredentialsProvider

trait RasterRDDWriterProvider[K] {
  def writer(credentialsProvider: AWSCredentialsProvider, bucket: String, layerPath: String, clobber: Boolean = true)
            (implicit sc: SparkContext): RasterRDDWriter[K]
}