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

  def apply[K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec: ClassTag, M: JsonFormat, C <: RDD[(K, V)]](
    attributeStore: S3AttributeStore,
    keyIndexMethod: KeyIndexMethod[K],
    options: Options
  )(implicit sc: SparkContext, bridge: Bridge[(RDD[(K, V)], M), C]): LayerReindexer[LayerId] = {
    val layerReader    = S3LayerReader[K, V, M, C](attributeStore, options.getCache)
    val layerDeleter   = S3LayerDeleter(attributeStore)
    val layerWriter    = S3LayerWriter[K, V, M, C](attributeStore, keyIndexMethod, options)

    val (bucket, prefix) = (attributeStore.bucket, attributeStore.prefix)
    val layerCopier = new SparkLayerCopier[S3LayerHeader, K, V, M, C](
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

  def apply[K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec: ClassTag, M: JsonFormat, C <: RDD[(K, V)]](
    attributeStore: S3AttributeStore,
    keyIndexMethod: KeyIndexMethod[K]
  )(implicit sc: SparkContext, bridge: Bridge[(RDD[(K, V)], M), C]): LayerReindexer[LayerId] =
    apply[K, V, M, C](attributeStore, keyIndexMethod, Options.DEFAULT)

  def apply[K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec: ClassTag, M: JsonFormat, C <: RDD[(K, V)]](
    bucket: String,
    prefix: String,
    keyIndexMethod: KeyIndexMethod[K],
    options: Options
  )
   (implicit sc: SparkContext, bridge: Bridge[(RDD[(K, V)], M), C]): LayerReindexer[LayerId] =
    apply[K, V, M, C](S3AttributeStore(bucket, prefix), keyIndexMethod, options)

  def apply[K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec: ClassTag, M: JsonFormat, C <: RDD[(K, V)]](
    bucket: String,
    prefix: String,
    keyIndexMethod: KeyIndexMethod[K])
   (implicit sc: SparkContext, bridge: Bridge[(RDD[(K, V)], M), C]): LayerReindexer[LayerId] =
    apply[K, V, M, C](S3AttributeStore(bucket, prefix), keyIndexMethod, Options.DEFAULT)
}
