package geotrellis.spark.io.s3

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.avro.AvroRecordCodec
import geotrellis.spark.io.avro.codecs._
import geotrellis.spark.io.index._
import geotrellis.spark.merge._
import geotrellis.util._

import com.typesafe.scalalogging.slf4j._
import org.apache.avro.Schema
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import spray.json._

import scala.reflect._

class S3LayerUpdater(
  val attributeStore: AttributeStore,
  layerReader: S3LayerReader
) extends LayerUpdater[LayerId] with LazyLogging {

  def rddWriter: S3RDDWriter = S3RDDWriter

  protected def _update[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]: Mergable
  ](id: LayerId, rdd: RDD[(K, V)] with Metadata[M], keyBounds: KeyBounds[K], mergeFunc: (V, V) => V) = {
    if (!attributeStore.layerExists(id)) throw new LayerNotFoundError(id)

    val LayerAttributes(header, metadata, keyIndex, writerSchema) = try {
      attributeStore.readLayerAttributes[S3LayerHeader, M, K](id)
    } catch {
      case e: AttributeNotFoundError => throw new LayerUpdateError(id).initCause(e)
    }

    if (!(keyIndex.keyBounds contains keyBounds))
      throw new LayerOutOfKeyBoundsError(id, keyIndex.keyBounds)

    val prefix = header.key
    val bucket = header.bucket

    val maxWidth = Index.digits(keyIndex.toIndex(keyIndex.keyBounds.maxKey))
    val keyPath = (key: K) => makePath(prefix, Index.encode(keyIndex.toIndex(key), maxWidth))

    logger.info(s"Saving updated RDD for layer ${id} to $bucket $prefix")
    val existingTiles =
      if(schemaHasChanged[K, V](writerSchema)) {
        logger.warn(s"RDD schema has changed, this requires rewriting the entire layer.")
        layerReader
          .read[K, V, M](id)

      } else {
        val query =
          new LayerQuery[K, M]
            .where(Intersects(rdd.metadata.getComponent[Bounds[K]].get))

        layerReader.read[K, V, M](id, query, layerReader.defaultNumPartitions, filterIndexOnly = true)
      }

    val updatedMetadata: M =
      metadata.merge(rdd.metadata)

    val updatedRdd: RDD[(K, V)] =
      existingTiles
        .fullOuterJoin(rdd)
        .flatMapValues {
          case (Some(layerTile), Some(updateTile)) => Some(mergeFunc(layerTile, updateTile))
          case (Some(layerTile), _) => Some(layerTile)
          case (_, Some(updateTile)) => Some(updateTile)
          case _ => None
        }

    val codec  = KeyValueRecordCodec[K, V]
    val schema = codec.schema

    // Write updated metadata, and the possibly updated schema
    // Only really need to write the metadata and schema
    attributeStore.writeLayerAttributes(id, header, updatedMetadata, keyIndex, schema)
    rddWriter.write(updatedRdd, bucket, keyPath)
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
