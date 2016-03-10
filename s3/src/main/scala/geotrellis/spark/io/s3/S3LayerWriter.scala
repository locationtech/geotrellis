package geotrellis.spark.io.s3

import geotrellis.raster.{MultibandTile, Tile}
import geotrellis.spark._
import geotrellis.spark.io._
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
 * @param keyIndexMethod  Method used to convert RDD keys to SFC indexes
 * @param attributeStore  AttributeStore to be used for storing raster metadata
 * @tparam K              Type of RDD Key (ex: GridKey)
 * @tparam V              Type of RDD Value (ex: Tile or MultibandTile )
 * @tparam M              Type of Metadata associated with the RDD[(K,V)]
 */
class S3LayerWriter(
  val attributeStore: AttributeStore[JsonFormat],
  bucket: String,
  keyPrefix: String
) extends LayerWriter[LayerId] with LazyLogging {

  def rddWriter: S3RDDWriter = S3RDDWriter

  protected def _write[
    K: AvroRecordCodec: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: Component[?, Bounds[K]]
  ](id: LayerId, rdd: RDD[(K, V)] with Metadata[M], keyIndex: KeyIndex[K]): Unit = {
    require(!attributeStore.layerExists(id), s"$id already exists")
    implicit val sc = rdd.sparkContext
    val prefix = makePath(keyPrefix, s"${id.name}/${id.zoom}")
    val metadata = rdd.metadata
    val header = S3LayerHeader(
      keyClass = classTag[K].toString(),
      valueClass = classTag[V].toString(),
      bucket = bucket,
      key = prefix)

    val maxWidth = Index.digits(keyIndex.toIndex(keyIndex.keyBounds.maxKey))
    val keyPath = (key: K) => makePath(prefix, Index.encode(keyIndex.toIndex(key), maxWidth))
    val schema = KeyValueRecordCodec[K, V].schema

    try {
      attributeStore.writeLayerAttributes(id, header, metadata, keyIndex, schema)

      logger.info(s"Saving RDD ${id.name} to $bucket  $prefix")
      rddWriter.write(rdd, bucket, keyPath)
    } catch {
      case e: Exception => throw new LayerWriteError(id).initCause(e)
    }
  }
}

object S3LayerWriter {
  def apply(attributeStore: AttributeStore[JsonFormat], bucket: String, prefix: String): S3LayerWriter =
    new S3LayerWriter(attributeStore, bucket, prefix)

  def apply(attributeStore: S3AttributeStore): S3LayerWriter =
    apply(attributeStore, attributeStore.bucket, attributeStore.prefix)

  def apply(bucket: String, prefix: String): S3LayerWriter =
    apply(S3AttributeStore(bucket, prefix))

}
