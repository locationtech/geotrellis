package geotrellis.spark.io.s3

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.avro.AvroRecordCodec
import geotrellis.spark.io.index._
import geotrellis.spark.io.json._

import org.apache.avro.Schema
import org.apache.spark.rdd.RDD
import spray.json._
import com.typesafe.scalalogging.slf4j._
import scala.reflect._

class S3LayerUpdater[K: Boundable: JsonFormat: ClassTag, V: ClassTag, M: JsonFormat](
  val attributeStore: AttributeStore[JsonFormat],
  rddWriter: S3RDDWriter[K, V],
  clobber: Boolean = true) extends LayerUpdater[LayerId, K, V, M] with LazyLogging {
  type container = RDD[(K, V)] with Metadata[M]

  def getS3Client: () => S3Client = () => S3Client.default

  def update[I <: BoundedKeyIndex[K]: JsonFormat](id: LayerId, rdd: Container, format: JsonFormat[I]): Unit = {
    if (!attributeStore.layerExists(id)) throw new LayerNotFoundError(id)
    implicit val sc = rdd.sparkContext
    val (existingHeader, existingMetadata, existingKeyBounds, existingKeyIndex, existingSchema) = try {
      attributeStore.readLayerAttributes[S3LayerHeader, M, KeyBounds[K], I, Schema](id)
    } catch {
      case e: AttributeNotFoundError => throw new LayerUpdateError(id).initCause(e)
    }

    val keyIndexSpace = existingKeyIndex.keyBounds
    val boundable = implicitly[Boundable[K]]
    val keyBounds = try {
      boundable.getKeyBounds(rdd)
    } catch {
      case e: UnsupportedOperationException => throw new LayerUpdateError(id, ": empty rdd update").initCause(e)
    }

    if (!boundable.includes(keyBounds.minKey, keyIndexSpace) || !boundable.includes(keyBounds.maxKey, keyIndexSpace))
      throw new LayerOutOfKeyBoundsError(id)

    val prefix = existingHeader.key
    val bucket = existingHeader.bucket

    val maxWidth = Index.digits(existingKeyIndex.toIndex(existingKeyBounds.maxKey))
    val keyPath = (key: K) => makePath(prefix, Index.encode(existingKeyIndex.toIndex(key), maxWidth))

    try {
      logger.info(s"Saving RDD ${rdd.name} to $bucket  $prefix")
      attributeStore.writeLayerAttributes(
        id, existingHeader, existingMetadata, boundable.combine(existingKeyBounds, keyBounds), existingKeyIndex, rddWriter.schema
      )
      rddWriter.write(rdd, bucket, keyPath, oneToOne = false)
    } catch {
      case e: Exception => throw new LayerUpdateError(id).initCause(e)
    }
  }
}

object S3LayerUpdater {
  def custom[K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec: ClassTag, M: JsonFormat](
    bucket: String,
    prefix: String,
    clobber: Boolean = true): S3LayerUpdater[K, V, M] =
    new S3LayerUpdater[K, V, M](
      S3AttributeStore(bucket, prefix),
      new S3RDDWriter[K, V],
      clobber
    )
}
