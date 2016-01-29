package geotrellis.spark.io.s3

import geotrellis.raster.{MultiBandTile, Tile}
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.json._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.avro.codecs._
import geotrellis.spark.io.index._
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
 */
class S3LayerWriter(
    val attributeStore: AttributeStore[JsonFormat],
    bucket: String,
    keyPrefix: String,
    clobber: Boolean = true,
    oneToOne: Boolean = false
) extends LayerWriter[LayerId] with LazyLogging {

  def write[
    K: AvroRecordCodec: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat
  ](id: LayerId, rdd: RDD[(K, V)] with Metadata[M], keyIndex: KeyIndex[K], keyBounds: KeyBounds[K]): Unit = {
    require(!attributeStore.layerExists(id) || clobber, s"$id already exists")
    implicit val sc = rdd.sparkContext
    val prefix = makePath(keyPrefix, s"${id.name}/${id.zoom}")
    val metadata = rdd.metadata
    val header = S3LayerHeader(
      keyClass = classTag[K].toString(),
      valueClass = classTag[V].toString(),
      bucket = bucket,
      key = prefix)

    val maxWidth = Index.digits(keyIndex.toIndex(keyBounds.maxKey))
    val keyPath = (key: K) => makePath(prefix, Index.encode(keyIndex.toIndex(key), maxWidth))
    val schema = KeyValueRecordCodec[K, V].schema

    try {
      attributeStore.writeLayerAttributes(id, header, metadata, keyBounds, keyIndex, schema)

      logger.info(s"Saving RDD ${id.name} to $bucket  $prefix")
      S3RDDWriter.write(rdd, bucket, keyPath, oneToOne = oneToOne)
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

  def apply(attributeStore: S3AttributeStore, options: Options): S3LayerWriter =
    new S3LayerWriter(
      attributeStore,
      attributeStore.bucket,
      attributeStore.prefix,
      options.clobber,
      options.oneToOne
    )

  def apply(attributeStore: S3AttributeStore): S3LayerWriter =
    apply(attributeStore, Options.DEFAULT)

  def apply(bucket: String, prefix: String, options: Options): S3LayerWriter =
    apply(S3AttributeStore(bucket, prefix), options)

  def apply(bucket: String, prefix: String): S3LayerWriter =
    apply(bucket, prefix, Options.DEFAULT)
}
