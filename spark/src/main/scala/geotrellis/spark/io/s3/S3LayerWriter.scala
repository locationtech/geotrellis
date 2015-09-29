package geotrellis.spark.io.s3

import geotrellis.raster.Tile
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.avro.codecs.KeyValueRecordCodec
import geotrellis.spark.io.json._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.avro.codecs._
import geotrellis.spark.io.index.KeyIndexMethod
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
 * @tparam V       Type of RDD Value (ex: Tile or MultiBandTile )
 * @tparam Container      Type of RDD Container that composes RDD and it's metadata (ex: RasterRDD or MultiBandRasterRDD)
 */
class S3LayerWriter[K: Boundable: JsonFormat: ClassTag, V: ClassTag, Container](
    val attributeStore: S3AttributeStore,
    rddWriter: S3RDDWriter[K, V],
    keyIndexMethod: KeyIndexMethod[K],
    bucket: String,
    keyPrefix: String,
    clobber: Boolean = true)
  (implicit val cons: ContainerConstructor[K, V, Container])
  extends Writer[LayerId, Container with RDD[(K, V)]] with LazyLogging {

  def getS3Client: ()=>S3Client = () => S3Client.default

  def write(id: LayerId, rdd: Container with RDD[(K, V)]) = {
    try {
      require(!attributeStore.layerExists(id) || clobber, s"$id already exists")
      implicit val sc = rdd.sparkContext
      val prefix = makePath(keyPrefix, s"${id.name}/${id.zoom}")
      val rasterMetaData = cons.getMetaData(rdd)
      val layerMetaData = S3LayerMetaData(
        layerId = id,
        keyClass = classTag[K].toString(),
        valueClass = classTag[K].toString(),
        bucket = bucket,
        key = prefix)

      val keyBounds = implicitly[Boundable[K]].getKeyBounds(rdd.asInstanceOf[RDD[(K, V)]])
      val keyIndex = keyIndexMethod.createIndex(keyBounds)

      attributeStore.cacheWrite(id, Fields.layerMetaData, layerMetaData)
      attributeStore.cacheWrite(id, Fields.rddMetadata, rasterMetaData)(cons.metaDataFormat)
      attributeStore.cacheWrite(id, Fields.keyBounds, keyBounds)
      attributeStore.cacheWrite(id, Fields.keyIndex, keyIndex)
      attributeStore.cacheWrite(id, Fields.schema, rddWriter.schema.toString.parseJson)

      val maxWidth = maxIndexWidth(keyIndex.toIndex(keyBounds.maxKey))
      val keyPath = (key: K) => makePath(prefix, encodeIndex(keyIndex.toIndex(key), maxWidth))

      logger.info(s"Saving RDD ${rdd.name} to $bucket  $prefix")
      rddWriter.write(rdd, bucket, keyPath, oneToOne = false)
    } catch {
      case e: Exception => throw new LayerWriteError(id).initCause(e)
    }
  }
}

object S3LayerWriter {
  def apply[K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec: ClassTag, Container[_]](
      bucket: String,
      prefix: String,
      keyIndexMethod: KeyIndexMethod[K],
      clobber: Boolean = true)
    (implicit cons: ContainerConstructor[K, V, Container[K]]): S3LayerWriter[K, V, Container[K]] =
    new S3LayerWriter(
      S3AttributeStore(bucket, prefix),
      new S3RDDWriter[K, V](),
      keyIndexMethod, bucket, prefix, clobber)
}