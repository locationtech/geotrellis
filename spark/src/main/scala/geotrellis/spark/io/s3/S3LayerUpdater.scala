package geotrellis.spark.io.s3

import com.typesafe.scalalogging.slf4j._
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.avro.AvroRecordCodec
import geotrellis.spark.io.index.KeyIndex
import geotrellis.spark.io.json._
import org.apache.avro.Schema
import org.apache.spark.rdd.RDD
import spray.json._
import scala.reflect._

class S3LayerUpdater[K: Boundable: JsonFormat: ClassTag, V: ClassTag, M: JsonFormat, C <: RDD[(K, V)]](
    val attributeStore: AttributeStore[JsonFormat],
    rddWriter: S3RDDWriter[K, V],
    clobber: Boolean = true)
  (implicit bridge: Bridge[(RDD[(K, V)], M), C])
  extends LayerUpdater[LayerId, K, V, M, C] with LazyLogging {

  def getS3Client: () => S3Client = () => S3Client.default

  def update(id: LayerId, rdd: C) = {
    if (!attributeStore.layerExists(id)) throw new LayerNotFoundError(id)
    implicit val sc = rdd.sparkContext
    val (existingHeader, _, existingKeyBounds, existingKeyIndex, _) = try {
      attributeStore.readLayerAttributes[S3LayerHeader, M, KeyBounds[K], KeyIndex[K], Schema](id)
    } catch {
      case e: AttributeNotFoundError => throw new LayerUpdateError(id).initCause(e)
    }

    val boundable = implicitly[Boundable[K]]
    val keyBounds = try {
      boundable.getKeyBounds(rdd)
    } catch {
      case e: UnsupportedOperationException => throw new LayerUpdateError(id, ": empty rdd update").initCause(e)
    }

    if (!boundable.includes(keyBounds.minKey, existingKeyBounds) || !boundable.includes(keyBounds.maxKey, existingKeyBounds))
      throw new LayerOutOfKeyBoundsError(id)

    val prefix = existingHeader.key
    val bucket = existingHeader.bucket

    val maxWidth = maxIndexWidth(existingKeyIndex.toIndex(existingKeyBounds.maxKey))
    val keyPath = (key: K) => makePath(prefix, encodeIndex(existingKeyIndex.toIndex(key), maxWidth))

    logger.info(s"Saving RDD ${rdd.name} to $bucket  $prefix")
    rddWriter.write(rdd, bucket, keyPath, oneToOne = false)
  }
}

object S3LayerUpdater {
  def apply[K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec: ClassTag, M: JsonFormat, C <: RDD[(K, V)]](
      bucket: String,
      prefix: String,
      clobber: Boolean = true)
    (implicit bridge: Bridge[(RDD[(K, V)], M), C]): S3LayerUpdater[K, V, M, C] =
    new S3LayerUpdater[K, V, M, C](
      S3AttributeStore(bucket, prefix),
      new S3RDDWriter[K, V],
      clobber
    )
}
