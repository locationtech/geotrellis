package geotrellis.spark.io.s3

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.avro.AvroRecordCodec
import geotrellis.spark.io.index._
import geotrellis.spark.io.json._

import com.typesafe.scalalogging.slf4j._
import org.apache.avro.Schema
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import spray.json._

import scala.reflect._

class S3LayerUpdater(
  val attributeStore: AttributeStore[JsonFormat],
  layerReader: S3LayerReader
) extends LayerUpdater[LayerId] with LazyLogging {

  def rddWriter: S3RDDWriter = S3RDDWriter

  protected def _update[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: Component[?, Bounds[K]]
  ](id: LayerId, rdd: RDD[(K, V)] with Metadata[M], keyBounds: KeyBounds[K]) = {
    if (!attributeStore.layerExists(id)) throw new LayerNotFoundError(id)

    val (existingHeader, metadata, keyIndex, writerSchema) = try {
      attributeStore.readLayerAttributes[S3LayerHeader, M, KeyIndex[K], Schema](id)
    } catch {
      case e: AttributeNotFoundError => throw new LayerUpdateError(id).initCause(e)
    }

    if (!(keyIndex.keyBounds contains keyBounds))
      throw new LayerOutOfKeyBoundsError(id, keyIndex.keyBounds)

    val prefix = existingHeader.key
    val bucket = existingHeader.bucket

    val maxWidth = Index.digits(keyIndex.toIndex(keyIndex.keyBounds.maxKey))
    val keyPath = (key: K) => makePath(prefix, Index.encode(keyIndex.toIndex(key), maxWidth))

    logger.info(s"Saving updated RDD for layer ${id} to $bucket $prefix")
    if(schemaHasChanged[K, V](writerSchema)) {
      logger.warn(s"RDD schema has changed, this requires rewriting the entire layer.")
      val entireLayer = layerReader.read[K, V, M](id)
      val updated: RDD[(K, V)] with Metadata[M] =
        entireLayer.withContext { allTiles =>
          allTiles
            .leftOuterJoin(rdd)
            .mapValues { case (layerTile, updateTile) =>
              updateTile.getOrElse(layerTile)
            }
        }

      rddWriter.write(updated, bucket, keyPath)
    } else {
      rddWriter.write(rdd, bucket, keyPath)
    }
  }
}

object S3LayerUpdater {
  def apply(
      bucket: String,
      prefix: String
  )(implicit sc: SparkContext): S3LayerUpdater =
    new S3LayerUpdater(
      S3AttributeStore(bucket, prefix),
      S3LayerReader(bucket, prefix)
    )
}
