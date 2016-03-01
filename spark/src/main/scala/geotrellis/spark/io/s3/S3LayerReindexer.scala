package geotrellis.spark.io.s3

import geotrellis.spark.utils.cache.Cache
import geotrellis.spark.{LayerId, Boundable}
import geotrellis.spark.io.avro._
import geotrellis.spark.io.index.KeyIndexMethod
import geotrellis.spark.io._

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import spray.json.JsonFormat

import scala.reflect.ClassTag

object S3LayerReindexer {
  case class Options(getCache: Option[LayerId => Cache[Long, Array[Byte]]], clobber: Boolean, oneToOne: Boolean)
  object Options {
    def DEFAULT = Options(None, true, false)

    implicit def toWriterOptions(opts: Options): S3LayerWriter.Options =
      S3LayerWriter.Options(opts.clobber, opts.oneToOne)
  }

  def apply(
    attributeStore: S3AttributeStore,
    options: Options
  )(implicit sc: SparkContext): LayerReindexer[LayerId] = {
    val layerReader  = S3LayerReader(attributeStore, options.getCache)
    val layerWriter  = S3LayerWriter(attributeStore, options)
    val layerDeleter = S3LayerDeleter(attributeStore)
    val layerCopier  = S3LayerCopier(attributeStore)

    GenericLayerReindexer[S3LayerHeader](attributeStore, layerReader, layerWriter, layerDeleter, layerCopier)
  }

  def apply(attributeStore: S3AttributeStore)(implicit sc: SparkContext): LayerReindexer[LayerId] =
    apply(attributeStore, Options.DEFAULT)

  def apply(
    bucket: String,
    prefix: String,
    options: Options
  )(implicit sc: SparkContext): LayerReindexer[LayerId] =
    apply(S3AttributeStore(bucket, prefix), options)

  def apply(bucket: String, prefix: String)(implicit sc: SparkContext): LayerReindexer[LayerId] =
    apply(S3AttributeStore(bucket, prefix), Options.DEFAULT)
}
