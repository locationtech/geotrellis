package geotrellis.spark.io.s3.spatial

import geotrellis.spark._
import geotrellis.raster._
import geotrellis.spark.utils.KryoSerializer
import geotrellis.spark.io.index._
import geotrellis.spark.io.s3._
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import java.io.ByteArrayInputStream
import com.amazonaws.services.s3.model.{PutObjectRequest, PutObjectResult}
import com.amazonaws.auth.{AWSCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.services.s3.model.ObjectMetadata
import geotrellis.spark.io.index.zcurve.Z2
import com.typesafe.scalalogging.slf4j._
import scala.collection.mutable.ArrayBuffer
import com.amazonaws.services.s3.model.AmazonS3Exception
import scala.reflect.ClassTag

object SpatialRasterRDDWriter extends RasterRDDWriter[SpatialKey] with LazyLogging {
  val encodeKey = (key: SpatialKey, ki: KeyIndex[SpatialKey]) =>
    f"${ki.toIndex(key)}%019d"
}
