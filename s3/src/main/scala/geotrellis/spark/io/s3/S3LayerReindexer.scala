package geotrellis.spark.io.s3

import geotrellis.spark.util.cache.Cache
import geotrellis.spark.{LayerId, Boundable}
import geotrellis.spark.io.avro._
import geotrellis.spark.io.index.KeyIndexMethod
import geotrellis.spark.io._
import geotrellis.util._

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import spray.json.JsonFormat

import scala.reflect.ClassTag

object S3LayerReindexer {
  def apply(attributeStore: S3AttributeStore)(implicit sc: SparkContext): LayerReindexer[LayerId] = {
    val layerReader  = S3LayerReader(attributeStore)
    val layerWriter  = S3LayerWriter(attributeStore)
    val layerDeleter = S3LayerDeleter(attributeStore)
    val layerCopier  = S3LayerCopier(attributeStore)

    GenericLayerReindexer[S3LayerHeader](attributeStore, layerReader, layerWriter, layerDeleter, layerCopier)
  }
  def apply(
    bucket: String,
    prefix: String
  )(implicit sc: SparkContext): LayerReindexer[LayerId] =
    apply(S3AttributeStore(bucket, prefix))
}
