package geotrellis.spark.io.s3

import geotrellis.raster.{MultibandTile, Tile}
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.json._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.avro.codecs._
import geotrellis.spark.io.index._
import org.apache.spark.rdd.RDD
import spray.json._
import com.typesafe.scalalogging.slf4j._
import scala.reflect._

/**
 * Handles writing Raster RDDs and their metadata to S3.
 *
 * @param bucket          S3 bucket to be written to
 * @param keyPrefix       S3 prefix to write the raster to
 * @param clobber         flag to overwrite raster if already present on S3
 * @param attributeStore  AttributeStore to be used for storing raster metadata
 * @tparam K              Type of RDD Key (ex: SpatialKey)
 * @tparam V              Type of RDD Value (ex: Tile or MultibandTile )
 * @tparam M              Type of Metadata associated with the RDD[(K,V)]
 */
class S3LayerWriter[K: Boundable: JsonFormat: ClassTag, V: ClassTag, M: JsonFormat](
    val attributeStore: AttributeStore[JsonFormat],
    rddWriter: S3RDDWriter[K, V],
    bucket: String,
    keyPrefix: String,
    clobber: Boolean = true,
    oneToOne: Boolean = false)
  extends Writer[LayerId, K, RDD[(K, V)] with Metadata[M]] with LazyLogging {

  def getS3Client: () => S3Client = () => S3Client.default

  def write[I <: KeyIndex[K]: JsonFormat](id: LayerId, rdd: RDD[(K, V)] with Metadata[M], keyIndex: I): Unit = {
    require(!attributeStore.layerExists(id) || clobber, s"$id already exists")
    implicit val sc = rdd.sparkContext
    val prefix = makePath(keyPrefix, s"${id.name}/${id.zoom}")
    val metadata = rdd.metadata
    val header = S3LayerHeader(
      keyClass = classTag[K].toString(),
      valueClass = classTag[V].toString(),
      bucket = bucket,
      key = prefix)

    val keyBounds = implicitly[Boundable[K]].getKeyBounds(rdd)
    val maxWidth = Index.digits(keyIndex.toIndex(keyBounds.maxKey))
    val keyPath = (key: K) => makePath(prefix, Index.encode(keyIndex.toIndex(key), maxWidth))

    try {
      attributeStore.writeLayerAttributes(id, header, metadata, keyBounds, keyIndex, rddWriter.schema)

      logger.info(s"Saving RDD ${id.name} to $bucket  $prefix")
      rddWriter.write(rdd, bucket, keyPath, oneToOne = oneToOne)
    } catch {
      case e: Exception => throw new LayerWriteError(id).initCause(e)
    }
  }

  def write(id: LayerId, rdd: RDD[(K, V)] with Metadata[M], keyIndexMethod: KeyIndexMethod[K]): Unit = {
    val keyBounds = implicitly[Boundable[K]].getKeyBounds(rdd)
    write(id, rdd, keyIndexMethod.createIndex(keyBounds))
  }
}

object S3LayerWriter {
  case class Options(clobber: Boolean, oneToOne: Boolean)
  object Options {
    def DEFAULT = Options(clobber = true, oneToOne = false)
  }

  def apply[
    K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, 
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat
  ](attributeStore: S3AttributeStore, options: Options): S3LayerWriter[K, V, M] =
    new S3LayerWriter[K, V, M](
      attributeStore,
      new S3RDDWriter[K, V],
      attributeStore.bucket,
      attributeStore.prefix,
      options.clobber,
      options.oneToOne
    )

  def apply[
    K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, 
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat
  ](attributeStore: S3AttributeStore): S3LayerWriter[K, V, M] =
    apply[K, V, M](attributeStore, Options.DEFAULT)

  def apply[
    K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, 
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat
  ](bucket: String, prefix: String, options: Options): S3LayerWriter[K, V, M] =
    apply[K, V, M](S3AttributeStore(bucket, prefix), options)

  def apply[
    K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, 
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat
  ](bucket: String, prefix: String): S3LayerWriter[K, V, M] =
    apply[K, V, M](bucket, prefix, Options.DEFAULT)

  def spatial(
    bucket: String,
    prefix: String,
    clobber: Boolean = true,
    oneToOne: Boolean = false
  ): S3LayerWriter[SpatialKey, Tile, RasterMetadata] =
    apply[SpatialKey, Tile, RasterMetadata](bucket, prefix, Options(clobber, oneToOne))

  def spatialMultiband(
    bucket: String,
    prefix: String,
    clobber: Boolean = true,
    oneToOne: Boolean = false
  ): S3LayerWriter[SpatialKey, MultibandTile, RasterMetadata] =
    apply[SpatialKey, MultibandTile, RasterMetadata](bucket, prefix, Options(clobber, oneToOne))

  def spaceTime(
    bucket: String,
    prefix: String,
    clobber: Boolean = true,
    oneToOne: Boolean = false
  ): S3LayerWriter[SpaceTimeKey, Tile, RasterMetadata] =
    apply[SpaceTimeKey, Tile, RasterMetadata](bucket, prefix, Options(clobber, oneToOne))

  def spaceTimeMultiband(
    bucket: String,
    prefix: String,
    clobber: Boolean = true,
    oneToOne: Boolean = false
  ): S3LayerWriter[SpaceTimeKey, MultibandTile, RasterMetadata] =
    apply[SpaceTimeKey, MultibandTile, RasterMetadata](bucket, prefix, Options(clobber, oneToOne))
}
