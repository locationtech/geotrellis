package geotrellis.spark.io.hbase

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.avro.AvroRecordCodec
import geotrellis.spark.io.avro.codecs._
import geotrellis.spark.merge._
import geotrellis.util._

import org.apache.spark.SparkContext
import com.typesafe.scalalogging.slf4j._
import org.apache.spark.rdd.RDD
import spray.json._

import scala.reflect._

class HBaseLayerUpdater(
  val instance: HBaseInstance,
  val attributeStore: AttributeStore,
  layerReader: HBaseLayerReader
) extends LayerUpdater[LayerId] with LazyLogging {

  protected def _update[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]: Mergable
  ](id: LayerId, rdd: RDD[(K, V)] with Metadata[M], keyBounds: KeyBounds[K], mergeFunc: (V, V) => V) = {
    if (!attributeStore.layerExists(id)) throw new LayerNotFoundError(id)

    val LayerAttributes(header, metadata, keyIndex, writerSchema) = try {
      attributeStore.readLayerAttributes[HBaseLayerHeader, M, K](id)
    } catch {
      case e: AttributeNotFoundError => throw new LayerUpdateError(id).initCause(e)
    }

    val table = header.tileTable

    if (!(keyIndex.keyBounds contains keyBounds))
      throw new LayerOutOfKeyBoundsError(id, keyIndex.keyBounds)

    val encodeKey = (key: K) => keyIndex.toIndex(key)

    logger.info(s"Saving updated RDD for layer ${id} to table $table")
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
    HBaseRDDWriter.write(updatedRdd, instance, id, encodeKey, table)
  }
}

object HBaseLayerUpdater {
  def apply(instance: HBaseInstance)(implicit sc: SparkContext): HBaseLayerUpdater =
    new HBaseLayerUpdater(
      instance = instance,
      attributeStore = HBaseAttributeStore(instance),
      layerReader = HBaseLayerReader(instance)
    )

  def apply(attributeStore: HBaseAttributeStore)(implicit sc: SparkContext): HBaseLayerUpdater =
    new HBaseLayerUpdater(
      instance = attributeStore.instance,
      attributeStore = attributeStore,
      layerReader = HBaseLayerReader(attributeStore)
    )
}
