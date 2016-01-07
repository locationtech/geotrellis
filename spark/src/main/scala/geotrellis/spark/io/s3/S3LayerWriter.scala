package geotrellis.spark.io.s3

import geotrellis.raster.{MultiBandTile, Tile}
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.json._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.avro.codecs._
import geotrellis.spark.io.index.{KeyIndex, KeyIndexMethod}

import org.apache.spark.rdd.RDD
import spray.json._
import com.typesafe.scalalogging.slf4j._
import scala.reflect._

/**
 * Handles writing Raster RDDs and their metadata to S3.
 *
 * @param bucket          S3 bucket to be written to
 * @param keyPrefix       S3 prefix to write the raster to
 * @param keyIndexMethod  Method used to convert RDD keys to SFC indexes
 * @param clobber         flag to overwrite raster if already present on S3
 * @param attributeStore  AttributeStore to be used for storing raster metadata
 * @tparam K              Type of RDD Key (ex: SpatialKey)
 * @tparam V              Type of RDD Value (ex: Tile or MultiBandTile )
 * @tparam M              Type of Metadata associated with the RDD[(K,V)]
 */
class S3LayerWriter[K: Boundable: JsonFormat: ClassTag, V: ClassTag, M: JsonFormat, I <: KeyIndex[K]: JsonFormat](
    val attributeStore: AttributeStore[JsonFormat],
    rddWriter: S3RDDWriter[K, V],
    keyIndexMethod: KeyIndexMethod[K, I],
    bucket: String,
    keyPrefix: String,
    clobber: Boolean = true,
    oneToOne: Boolean = false)
  extends Writer[LayerId, RDD[(K, V)] with Metadata[M]] with LazyLogging {

  def getS3Client: () => S3Client = () => S3Client.default

  def write(id: LayerId, rdd: RDD[(K, V)] with Metadata[M]) = {
    require(!attributeStore.layerExists(id) || clobber, s"$id already exists")
    implicit val sc = rdd.sparkContext
    val prefix = makePath(keyPrefix, s"${id.name}/${id.zoom}")
    val metadata = rdd.metadata
    val header = S3LayerHeader(
      keyClass = classTag[K].toString(),
      valueClass = classTag[K].toString(),
      bucket = bucket,
      key = prefix)

    val keyBounds = implicitly[Boundable[K]].getKeyBounds(rdd)
    val keyIndex = keyIndexMethod.createIndex(keyBounds)
    val maxWidth = maxIndexWidth(keyIndex.toIndex(keyBounds.maxKey))
    val keyPath = (key: K) => makePath(prefix, encodeIndex(keyIndex.toIndex(key), maxWidth))

    try {
      attributeStore.writeLayerAttributes(id, header, metadata, keyBounds, keyIndex, rddWriter.schema)

      logger.info(s"Saving RDD ${id.name} to $bucket  $prefix")
      rddWriter.write(rdd, bucket, keyPath, oneToOne = oneToOne)
    } catch {
      case e: Exception => throw new LayerWriteError(id).initCause(e)
    }
  }
}

object S3LayerWriter {
  case class Options(clobber: Boolean, oneToOne: Boolean)
  object Options {
    def DEFAULT = Options(clobber = true, oneToOne = false)
  }

  def apply[
    K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec: ClassTag,
    M: JsonFormat, I <: KeyIndex[K]: JsonFormat
  ](attributeStore: S3AttributeStore, keyIndexMethod: KeyIndexMethod[K, I], options: Options): S3LayerWriter[K, V, M, I] =
    new S3LayerWriter[K, V, M, I](
      attributeStore,
      new S3RDDWriter[K, V],
      keyIndexMethod,
      attributeStore.bucket,
      attributeStore.prefix,
      options.clobber,
      options.oneToOne
    )

  def apply[
    K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec: ClassTag,
    M: JsonFormat, C <: RDD[(K, V)], I <: KeyIndex[K]: JsonFormat
  ](attributeStore: S3AttributeStore, keyIndexMethod: KeyIndexMethod[K, I]): S3LayerWriter[K, V, M, I] =
    apply[K, V, M, I](attributeStore, keyIndexMethod, Options.DEFAULT)

  def apply[
    K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec: ClassTag,
    M: JsonFormat, I <: KeyIndex[K]: JsonFormat
  ](bucket: String, prefix: String, keyIndexMethod: KeyIndexMethod[K, I], options: Options): S3LayerWriter[K, V, M, I] =
    apply[K, V, M, I](S3AttributeStore(bucket, prefix), keyIndexMethod, options)

  def apply[
    K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec: ClassTag,
    M: JsonFormat, I <: KeyIndex[K]: JsonFormat
  ](bucket: String, prefix: String, keyIndexMethod: KeyIndexMethod[K, I]): S3LayerWriter[K, V, M, I] =
    apply[K, V, M, I](bucket, prefix, keyIndexMethod, Options.DEFAULT)

  def spatial[I <: KeyIndex[SpatialKey]: JsonFormat](
    bucket: String,
    prefix: String,
    keyIndexMethod: KeyIndexMethod[SpatialKey, I],
    clobber: Boolean = true,
    oneToOne: Boolean = false
  ): S3LayerWriter[SpatialKey, Tile, RasterMetaData, I] =
    apply[SpatialKey, Tile, RasterMetaData, I](bucket, prefix, keyIndexMethod, Options(clobber, oneToOne))

  def spatialMultiBand[I <: KeyIndex[SpatialKey]: JsonFormat](
    bucket: String,
    prefix: String,
    keyIndexMethod: KeyIndexMethod[SpatialKey, I],
    clobber: Boolean = true,
    oneToOne: Boolean = false
  ): S3LayerWriter[SpatialKey, MultiBandTile, RasterMetaData, I] =
    apply[SpatialKey, MultiBandTile, RasterMetaData, I](bucket, prefix, keyIndexMethod, Options(clobber, oneToOne))

  def spaceTime[I <: KeyIndex[SpaceTimeKey]: JsonFormat](
    bucket: String,
    prefix: String,
    keyIndexMethod: KeyIndexMethod[SpaceTimeKey, I],
    clobber: Boolean = true,
    oneToOne: Boolean = false
  ): S3LayerWriter[SpaceTimeKey, Tile, RasterMetaData, I] =
    apply[SpaceTimeKey, Tile, RasterMetaData, I](bucket, prefix, keyIndexMethod, Options(clobber, oneToOne))

  def spaceTimeMultiBand[I <: KeyIndex[SpaceTimeKey]: JsonFormat](
    bucket: String,
    prefix: String,
    keyIndexMethod: KeyIndexMethod[SpaceTimeKey, I],
    clobber: Boolean = true,
    oneToOne: Boolean = false
  ): S3LayerWriter[SpaceTimeKey, MultiBandTile, RasterMetaData, I] =
    apply[SpaceTimeKey, MultiBandTile, RasterMetaData, I](bucket, prefix, keyIndexMethod, Options(clobber, oneToOne))
}
