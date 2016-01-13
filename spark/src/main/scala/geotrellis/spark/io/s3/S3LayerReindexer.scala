package geotrellis.spark.io.s3

import geotrellis.spark.utils.cache.Cache
import geotrellis.spark.{LayerId, Boundable}
import geotrellis.spark.io._
import geotrellis.spark.io.index.{KeyIndex, KeyIndexMethod}
import geotrellis.spark.io.avro._
import geotrellis.spark.io.json._

import org.apache.spark.SparkContext
import spray.json.JsonFormat

import scala.reflect.ClassTag

object S3LayerReindexer {
  case class Options(
    getCache: Option[LayerId => Cache[Long, Array[Byte]]],
    clobber: Boolean, oneToOne: Boolean, getS3Client: () => S3Client)
  object Options {
    def DEFAULT = Options(None, true, false, () => S3Client.default)

    implicit def toWriterOptions(opts: Options): S3LayerWriter.Options =
      S3LayerWriter.Options(opts.clobber, opts.oneToOne)
  }

  def apply[K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec: ClassTag, M: JsonFormat]
    (attributeStore: S3AttributeStore, options: Options)(implicit sc: SparkContext): LayerReindexer[LayerId, K] = {
    val layerReader  = new S3LayerReader[K, V, M](
      attributeStore = attributeStore,
      rddReader      = new S3RDDReader[K, V] { override val getS3Client: () => S3Client = options.getS3Client },
      getCache       = options.getCache
    )

    val layerDeleter = new S3LayerDeleter(attributeStore) { override val getS3Client: () => S3Client = options.getS3Client }
    val layerWriter  = new S3LayerWriter[K, V, M](
      attributeStore,
      new S3RDDWriter[K, V] { override val getS3Client: () => S3Client = options.getS3Client },
      attributeStore.bucket,
      attributeStore.prefix,
      options.clobber,
      options.oneToOne)

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

  def apply[K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec: ClassTag, M: JsonFormat]
    (attributeStore: S3AttributeStore)(implicit sc: SparkContext): LayerReindexer[LayerId, K] =
    apply[K, V, M](attributeStore, Options.DEFAULT)

  def apply[K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec: ClassTag, M: JsonFormat]
    (bucket: String, prefix: String, options: Options)(implicit sc: SparkContext): LayerReindexer[LayerId, K] =
    apply[K, V, M](S3AttributeStore(bucket, prefix), options)

  def apply[K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec: ClassTag, M: JsonFormat]
    (bucket: String, prefix: String)(implicit sc: SparkContext): LayerReindexer[LayerId, K] =
    apply[K, V, M](S3AttributeStore(bucket, prefix), Options.DEFAULT)
}
