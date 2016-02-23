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

  def apply[
    K: Boundable: AvroRecordCodec: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat
  ](
    attributeStore: S3AttributeStore,
    keyIndexMethod: KeyIndexMethod[K],
    options: Options
  )(implicit sc: SparkContext): LayerReindexer[LayerId] = {
    val layerReader  = S3LayerReader[K, V, M](attributeStore, options.getCache)
    val layerDeleter = S3LayerDeleter(attributeStore)
    val layerWriter  = S3LayerWriter[K, V, M](attributeStore, keyIndexMethod, options)

    val (bucket, prefix) = (attributeStore.bucket, attributeStore.prefix)
    val layerCopier = new SparkLayerCopier[S3LayerHeader, K, V, M](
      attributeStore = attributeStore,
      layerReader    = layerReader,
      layerWriter    = layerWriter
    ) {
      def headerUpdate(id: LayerId, header: S3LayerHeader): S3LayerHeader =
        header.copy(bucket, key = makePath(prefix, s"${id.name}/${id.zoom}"))
    }

    val layerMover = GenericLayerMover(layerCopier, layerDeleter)

    GenericLayerReindexer(layerDeleter, layerCopier, layerMover)
  }

  def apply[
    K: Boundable: AvroRecordCodec: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat
  ](attributeStore: S3AttributeStore, keyIndexMethod: KeyIndexMethod[K])(implicit sc: SparkContext): LayerReindexer[LayerId] =
    apply[K, V, M](attributeStore, keyIndexMethod, Options.DEFAULT)

  def apply[
    K: Boundable: AvroRecordCodec: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat
  ](
    bucket: String,
    prefix: String,
    keyIndexMethod: KeyIndexMethod[K],
    options: Options
  )(implicit sc: SparkContext): LayerReindexer[LayerId] =
    apply[K, V, M](S3AttributeStore(bucket, prefix), keyIndexMethod, options)

  def apply[
    K: Boundable: AvroRecordCodec: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat
  ](bucket: String, prefix: String, keyIndexMethod: KeyIndexMethod[K])(implicit sc: SparkContext): LayerReindexer[LayerId] =
    apply[K, V, M](S3AttributeStore(bucket, prefix), keyIndexMethod, Options.DEFAULT)
}
