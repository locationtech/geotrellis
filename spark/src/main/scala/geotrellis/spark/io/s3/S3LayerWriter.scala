package geotrellis.spark.io.s3

import geotrellis.raster.{MultiBandTile, Tile}
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.json._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.avro.codecs._
import geotrellis.spark.io.index.{KeyIndex, KeyIndexMethod}
import org.apache.avro.Schema
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import spray.json._
import spray.json.DefaultJsonProtocol._
import scala.reflect._
import com.typesafe.scalalogging.slf4j._
import AttributeStore.Fields

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
 * @tparam C              Type of RDD Container that composes RDD and it's metadata (ex: RasterRDD or MultiBandRasterRDD)
 */
class S3LayerWriter[K: Boundable: JsonFormat: ClassTag, V: ClassTag, M: JsonFormat, C <: RDD[(K, V)]](
    val attributeStore: AttributeStore[JsonFormat],
    rddWriter: S3RDDWriter[K, V],
    keyIndexMethod: KeyIndexMethod[K],
    bucket: String,
    keyPrefix: String,
    clobber: Boolean = true,
    oneToOne: Boolean = false)
  (implicit bridge: Bridge[(RDD[(K, V)], M), C])
  extends Writer[LayerId, C] with LazyLogging {

  def getS3Client: () => S3Client = () => S3Client.default

  def write(id: LayerId, rdd: C) = {
    require(!attributeStore.layerExists(id) || clobber, s"$id already exists")
    implicit val sc = rdd.sparkContext
    val prefix = makePath(keyPrefix, s"${id.name}/${id.zoom}")
    val (_, metadata) = bridge.unapply(rdd)
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
  def apply[K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec: ClassTag, M: JsonFormat, C <: RDD[(K, V)]](
      bucket: String,
      prefix: String,
      keyIndexMethod: KeyIndexMethod[K],
      clobber: Boolean = true,
      oneToOne: Boolean = false)
    (implicit bridge: Bridge[(RDD[(K, V)], M), C]): S3LayerWriter[K, V, M, C] =
    new S3LayerWriter[K, V, M, C](
      S3AttributeStore(bucket, prefix),
      new S3RDDWriter[K, V],
      keyIndexMethod, bucket, prefix, clobber, oneToOne)

  def spatial(bucket: String, prefix: String, keyIndexMethod: KeyIndexMethod[SpatialKey], clobber: Boolean = true, oneToOne: Boolean = false)
    (implicit sc: SparkContext, bridge: Bridge[(RDD[(SpatialKey, Tile)], RasterMetaData), RasterRDD[SpatialKey]]) =
    new S3LayerWriter[SpatialKey, Tile, RasterMetaData, RasterRDD[SpatialKey]](
      new S3AttributeStore(bucket, prefix), new S3RDDWriter[SpatialKey, Tile], keyIndexMethod, bucket, prefix, clobber, oneToOne)

  def spatialMultiBand(bucket: String, prefix: String, keyIndexMethod: KeyIndexMethod[SpatialKey], clobber: Boolean = true, oneToOne: Boolean = false)
    (implicit sc: SparkContext, bridge: Bridge[(RDD[(SpatialKey, MultiBandTile)], RasterMetaData), MultiBandRasterRDD[SpatialKey]]) =
    new S3LayerWriter[SpatialKey, MultiBandTile, RasterMetaData, MultiBandRasterRDD[SpatialKey]](
      new S3AttributeStore(bucket, prefix), new S3RDDWriter[SpatialKey, MultiBandTile], keyIndexMethod, bucket, prefix, clobber, oneToOne)

  def spaceTime(bucket: String, prefix: String, keyIndexMethod: KeyIndexMethod[SpaceTimeKey], clobber: Boolean = true, oneToOne: Boolean = false)
    (implicit sc: SparkContext, bridge: Bridge[(RDD[(SpaceTimeKey, Tile)], RasterMetaData), RasterRDD[SpaceTimeKey]]) =
    new S3LayerWriter[SpaceTimeKey, Tile, RasterMetaData, RasterRDD[SpaceTimeKey]](
      new S3AttributeStore(bucket, prefix), new S3RDDWriter[SpaceTimeKey, Tile], keyIndexMethod, bucket, prefix, clobber, oneToOne)

  def spaceTimeMultiBand(bucket: String, prefix: String, keyIndexMethod: KeyIndexMethod[SpaceTimeKey], clobber: Boolean = true, oneToOne: Boolean = false)
    (implicit sc: SparkContext, bridge: Bridge[(RDD[(SpaceTimeKey, MultiBandTile)], RasterMetaData), MultiBandRasterRDD[SpaceTimeKey]]) =
    new S3LayerWriter[SpaceTimeKey, MultiBandTile, RasterMetaData, MultiBandRasterRDD[SpaceTimeKey]](
      new S3AttributeStore(bucket, prefix), new S3RDDWriter[SpaceTimeKey, MultiBandTile], keyIndexMethod, bucket, prefix, clobber, oneToOne)
}
