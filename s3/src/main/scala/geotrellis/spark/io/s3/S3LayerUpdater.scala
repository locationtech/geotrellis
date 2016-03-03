package geotrellis.spark.io.s3

import com.typesafe.scalalogging.slf4j._
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.avro.AvroRecordCodec
import geotrellis.spark.io.index._
import geotrellis.spark.io.json._
import org.apache.avro.Schema
import org.apache.spark.rdd.RDD
import spray.json._
import scala.reflect._

// RETODO: Updater for File and Hadoop

class S3LayerUpdater(
    val attributeStore: AttributeStore[JsonFormat],
    clobber: Boolean = true)
  extends LayerUpdater[LayerId] with LazyLogging {

  def rddWriter: S3RDDWriter = S3RDDWriter

  def update[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat
  ](id: LayerId, rdd: RDD[(K, V)] with Metadata[M]) = {
    if (!attributeStore.layerExists(id)) throw new LayerNotFoundError(id)
    implicit val sc = rdd.sparkContext
    val (existingHeader, _, existingKeyBounds, existingKeyIndex, _) = try {
      attributeStore.readLayerAttributes[S3LayerHeader, M, KeyBounds[K], KeyIndex[K], Schema](id)
    } catch {
      case e: AttributeNotFoundError => throw new LayerUpdateError(id).initCause(e)
    }

    val boundable = implicitly[Boundable[K]]
    val keyBounds = Bounds.fromRdd(rdd).getOrElse(throw new LayerUpdateError(id, "empty rdd update"))

    if (!(existingKeyBounds includes keyBounds.minKey) || !(existingKeyBounds includes keyBounds.maxKey))
      throw new LayerOutOfKeyBoundsError(id)

    val prefix = existingHeader.key
    val bucket = existingHeader.bucket

    val maxWidth = Index.digits(existingKeyIndex.toIndex(existingKeyBounds.maxKey))
    val keyPath = (key: K) => makePath(prefix, Index.encode(existingKeyIndex.toIndex(key), maxWidth))

    logger.info(s"Saving RDD ${rdd.name} to $bucket  $prefix")
    rddWriter.write(rdd, bucket, keyPath, oneToOne = false)
  }
}

object S3LayerUpdater {
  def apply(
      bucket: String,
      prefix: String,
      clobber: Boolean = true
  ): S3LayerUpdater =
    new S3LayerUpdater(
      S3AttributeStore(bucket, prefix),
      clobber
    )
}
