package geotrellis.spark.io.s3

import geotrellis.spark.utils.cache.Cache
import geotrellis.spark.{LayerId, Boundable}
import geotrellis.spark.io.avro._
import geotrellis.spark.io.index.{KeyIndex, KeyIndexMethod}
import geotrellis.spark.io._

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

  def apply[
    K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec: ClassTag,
    M: JsonFormat, FI <: KeyIndex[K]: JsonFormat, TI <: KeyIndex[K]: JsonFormat
  ](attributeStore: S3AttributeStore,
    keyIndexMethod: KeyIndexMethod[K, TI],
    options: Options
  )(implicit sc: SparkContext): LayerReindexer[LayerId] = {
    val layerReaderFrom  = new S3LayerReader[K, V, M, FI](
      attributeStore = attributeStore,
      rddReader      = new S3RDDReader[K, V] { override val getS3Client: () => S3Client = options.getS3Client },
      getCache       = options.getCache
    )
    val layerReaderTo  = new S3LayerReader[K, V, M, TI](
      attributeStore = attributeStore,
      rddReader      = new S3RDDReader[K, V] { override val getS3Client: () => S3Client = options.getS3Client },
      getCache       = options.getCache
    )
    val layerDeleter = new S3LayerDeleter(attributeStore) { override val getS3Client: () => S3Client = options.getS3Client }
    val layerWriter  = new S3LayerWriter[K, V, M, TI](
      attributeStore,
      new S3RDDWriter[K, V] { override val getS3Client: () => S3Client = options.getS3Client },
      keyIndexMethod,
      attributeStore.bucket,
      attributeStore.prefix,
      options.clobber,
      options.oneToOne)
    val (bucket, prefix) = (attributeStore.bucket, attributeStore.prefix)
    val layerCopierFrom = new SparkLayerCopier[S3LayerHeader, K, V, M, TI](
      attributeStore = attributeStore,
      layerReader    = layerReaderFrom,
      layerWriter    = layerWriter
    ) {
      def headerUpdate(id: LayerId, header: S3LayerHeader): S3LayerHeader =
        header.copy(bucket, key = makePath(prefix, s"${id.name}/${id.zoom}"))
    }
    val layerCopierTo = new SparkLayerCopier[S3LayerHeader, K, V, M, TI](
      attributeStore = attributeStore,
      layerReader    = layerReaderTo,
      layerWriter    = layerWriter
    ) {
      def headerUpdate(id: LayerId, header: S3LayerHeader): S3LayerHeader =
        header.copy(bucket, key = makePath(prefix, s"${id.name}/${id.zoom}"))
    }
    val layerMover = GenericLayerMover(layerCopierTo, layerDeleter)
    GenericLayerReindexer(layerDeleter, layerCopierFrom, layerMover)
  }

  def apply[
    K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec: ClassTag,
    M: JsonFormat, FI <: KeyIndex[K]: JsonFormat, TI <: KeyIndex[K]: JsonFormat
  ](attributeStore: S3AttributeStore, keyIndexMethod: KeyIndexMethod[K, TI])(implicit sc: SparkContext): LayerReindexer[LayerId] =
    apply[K, V, M, FI, TI](attributeStore, keyIndexMethod, Options.DEFAULT)

  def apply[
    K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec: ClassTag,
    M: JsonFormat, FI <: KeyIndex[K]: JsonFormat, TI <: KeyIndex[K]: JsonFormat
  ](
    bucket: String,
    prefix: String,
    keyIndexMethod: KeyIndexMethod[K, TI],
    options: Options
  )(implicit sc: SparkContext): LayerReindexer[LayerId] =
    apply[K, V, M, FI, TI](S3AttributeStore(bucket, prefix), keyIndexMethod, options)

  def apply[
    K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec: ClassTag,
    M: JsonFormat, FI <: KeyIndex[K]: JsonFormat, TI <: KeyIndex[K]: JsonFormat
  ](bucket: String, prefix: String, keyIndexMethod: KeyIndexMethod[K, TI])(implicit sc: SparkContext): LayerReindexer[LayerId] =
    apply[K, V, M, FI, TI](S3AttributeStore(bucket, prefix), keyIndexMethod, Options.DEFAULT)
}
