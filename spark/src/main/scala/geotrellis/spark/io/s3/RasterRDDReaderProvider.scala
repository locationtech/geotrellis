package geotrellis.spark.io.s3

import geotrellis.spark._
import geotrellis.spark.io._

import org.apache.spark.SparkContext
import com.amazonaws.auth.AWSCredentialsProvider

trait RasterRDDReaderProvider[K] {
  def reader(credentialsProvider: AWSCredentialsProvider,layerMetaData: S3LayerMetaData)
            (implicit sc: SparkContext): FilterableRasterRDDReader[K]
}