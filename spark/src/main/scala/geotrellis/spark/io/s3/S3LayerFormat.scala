package geotrellis.spark.io.s3

import com.typesafe.scalalogging.slf4j._
import geotrellis.raster.mosaic.MergeView
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.avro.AvroRecordCodec
import geotrellis.spark.io.index.{KeyIndex, KeyIndexMethod}
import geotrellis.spark.io.json._
import geotrellis.spark.mosaic._
import org.apache.avro.Schema
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import spray.json._
import scala.reflect._

/**
 * Handles reading, writing and updating Raster RDDs and their metadata to S3.
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
class S3LayerFormat[K: Boundable: AvroRecordCodec: JsonFormat: ClassTag,
                    V: MergeView: AvroRecordCodec: ClassTag, Container](
    val attributeStore: AttributeStore[JsonFormat],
    keyIndexMethod: KeyIndexMethod[K],
    bucket: String,
    keyPrefix: String,
    rddReader: S3RDDReader[K, V],
    rddWriter: S3RDDWriter[K, V],
    clobber: Boolean = true,
    getCache: Option[LayerId => Cache[Long, Array[Byte]]] = None)
  (implicit sc: SparkContext, val cons: ContainerConstructor[K, V, Container])
  extends LayerFormat[LayerId, K, V, Container] with LazyLogging {

  lazy val layerReader = new S3LayerReader(attributeStore, rddReader, getCache)
  lazy val layerWriter = new S3LayerWriter(attributeStore, rddWriter, keyIndexMethod, bucket, keyPrefix, clobber)

  def getS3Client: () => S3Client = () => S3Client.default

  type MetaDataType = cons.MetaDataType

  val defaultNumPartitions = sc.defaultParallelism

  def update(id: LayerId, rdd: Container with RDD[(K, V)], numPartitions: Int) = {
    try {
      require(!attributeStore.layerExists(id) || clobber, s"$id already exists")
      implicit val mdFormat = cons.metaDataFormat
      val prefix = makePath(keyPrefix, s"${id.name}/${id.zoom}")
      val header = S3LayerHeader(
        keyClass = classTag[K].toString(),
        valueClass = classTag[K].toString(),
        bucket = bucket,
        key = prefix)

      val (existingHeader, existingMetaData, existingKeyBounds, existingKeyIndex, existingSchema) =
        attributeStore.readLayerAttributes[S3LayerHeader, MetaDataType, KeyBounds[K], KeyIndex[K], Schema](id)

      if(header !== existingHeader) throw new HeaderMatchError(id, existingHeader, header)

      val metadata = cons.getMetaData(rdd)
      val keyBounds = implicitly[Boundable[K]].getKeyBounds(rdd.asInstanceOf[RDD[(K, V)]])
      val keyIndex = keyIndexMethod.createIndex(keyBounds)

      val rasterQuery = new RDDQuery[K, MetaDataType].where(Intersects(keyBounds))
      val queryKeyBounds = rasterQuery(existingMetaData, existingKeyBounds)

      val existingMaxWidth = maxIndexWidth(keyIndex.toIndex(existingKeyBounds.maxKey))
      val existingKeyPath = (index: Long) => makePath(prefix, encodeIndex(index, existingMaxWidth))
      val decompose = (bounds: KeyBounds[K]) => keyIndex.indexRanges(bounds)
      val cache = getCache.map(f => f(id))
      val existing = rddReader.read(bucket, existingKeyPath, queryKeyBounds, decompose, Some(existingSchema), cache, numPartitions)

      val combinedMetaData = cons.combineMetaData(existingMetaData, metadata)
      val combinedKeyBounds = implicitly[Boundable[K]].combine(existingKeyBounds, keyBounds)
      val combinedRdd = existing merge rdd

      attributeStore.writeLayerAttributes(id, existingHeader, combinedMetaData, combinedKeyBounds, existingKeyIndex, existingSchema)

      val maxWidth = maxIndexWidth(keyIndex.toIndex(combinedKeyBounds.maxKey))
      val keyPath = (key: K) => makePath(prefix, encodeIndex(keyIndex.toIndex(key), maxWidth))

      logger.info(s"Saving RDD ${combinedRdd.name} to $bucket $prefix")
      rddWriter.write(combinedRdd, bucket, keyPath, oneToOne = false)
    } catch {
      case e: Exception => throw new LayerWriteError(id).initCause(e)
    }
  }
}

object S3LayerFormat {
  def apply[K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, V: MergeView: AvroRecordCodec: ClassTag, Container[_]](
    bucket: String,
    prefix: String,
    keyIndexMethod: KeyIndexMethod[K],
    clobber: Boolean = true,
    getCache: Option[LayerId => Cache[Long, Array[Byte]]] = None)
  (implicit sc: SparkContext, cons: ContainerConstructor[K, V, Container[K]]): S3LayerFormat[K, V, Container[K]] =
    new S3LayerFormat(
      attributeStore = S3AttributeStore(bucket, prefix),
      keyIndexMethod = keyIndexMethod,
      bucket         = bucket,
      keyPrefix      = prefix,
      rddReader      = new S3RDDReader[K, V],
      rddWriter      = new S3RDDWriter[K, V],
      clobber        = clobber,
      getCache       = getCache)
}
