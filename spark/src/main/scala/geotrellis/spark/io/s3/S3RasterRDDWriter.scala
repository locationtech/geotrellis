package geotrellis.spark.io.s3

import geotrellis.raster.Tile
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.avro.codecs.KeyValueRecordCodec
import geotrellis.spark.io.json._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.avro.codecs._
import geotrellis.spark.io.index.KeyIndexMethod
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
 * @tparam TileType       Type of RDD Value (ex: Tile or MultiBandTile )
 * @tparam Container      Type of RDD Container that composes RDD and it's metadata (ex: RasterRDD or MultiBandRasterRDD)
 */
class S3RasterRDDWriter[K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, TileType: AvroRecordCodec: ClassTag, Container[_]](
    bucket: String,
    keyPrefix: String,
    keyIndexMethod: KeyIndexMethod[K],
    clobber: Boolean = true)
  (val attributeStore: S3AttributeStore = S3AttributeStore(bucket, keyPrefix))
  (implicit val cons: ContainerConstructor[K, TileType, Container])
  extends Writer[LayerId,Container[K] with RDD[(K, TileType)]] with LazyLogging {

  val getS3Client: ()=>S3Client = () => S3Client.default

  def write(id: LayerId, rdd: Container[K] with RDD[(K, TileType)]) = {
    require(clobber, "S3 writer does not yet perform a clobber check") // TODO: Check for clobber
    implicit val sc = rdd.sparkContext
    val prefix = makePath(keyPrefix, s"${id.name}/${id.zoom}")
    val rasterMetaData = cons.getMetaData(rdd)
    val layerMetaData = S3LayerMetaData(
      layerId = id,
      keyClass = classTag[K].toString(),
      valueClass = classTag[K].toString(),
      bucket = bucket,
      key = prefix)

    val keyBounds = implicitly[Boundable[K]].getKeyBounds(rdd.asInstanceOf[RDD[(K, TileType)]])
    val keyIndex = keyIndexMethod.createIndex(keyBounds)

    attributeStore.cacheWrite(id, Fields.layerMetaData, layerMetaData)
    attributeStore.cacheWrite(id, Fields.layerMetaData, rasterMetaData)(cons.metaDataFormat)
    attributeStore.cacheWrite(id, Fields.keyBounds, keyBounds)
    attributeStore.cacheWrite(id, Fields.keyIndex, keyIndex)

    val maxWidth = maxIndexWidth(keyIndex.toIndex(keyBounds.maxKey))
    val keyPath = (index: Long) => makePath(prefix, encodeIndex(index, maxWidth))
    val codec = KeyValueRecordCodec[K, Tile]
    attributeStore.cacheWrite(id,"schema", codec.schema.toString.parseJson)

    logger.info(s"Saving RDD ${rdd.name} to $bucket  $prefix")
    new S3RDDWriter[K, TileType](bucket, getS3Client)
      .write(rdd, keyIndex, keyPath, oneToOne = false)
  }
}