package geotrellis.spark.io.accumulo

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.avro.AvroRecordCodec
import geotrellis.spark.io.index.KeyIndex
import geotrellis.spark.io.json._

import com.typesafe.scalalogging.slf4j._
import org.apache.avro.Schema
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import spray.json._

import scala.reflect._

import AccumuloLayerWriter.Options

class AccumuloLayerUpdater(
  val instance: AccumuloInstance,
  val attributeStore: AttributeStore[JsonFormat],
  layerReader: AccumuloLayerReader,
  options: Options
) extends LayerUpdater[LayerId] with LazyLogging {

  protected def _update[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: Component[?, Bounds[K]]
  ](id: LayerId, rdd: RDD[(K, V)] with Metadata[M], keyBounds: KeyBounds[K]) = {
    if (!attributeStore.layerExists(id)) throw new LayerNotFoundError(id)

    val (header, _, keyIndex, writerSchema) = try {
      attributeStore.readLayerAttributes[AccumuloLayerHeader, M, KeyIndex[K], Schema](id)
    } catch {
      case e: AttributeNotFoundError => throw new LayerUpdateError(id).initCause(e)
    }

    val table = header.tileTable

    if (!(keyIndex.keyBounds contains keyBounds))
      throw new LayerOutOfKeyBoundsError(id, keyIndex.keyBounds)

    val encodeKey = (key: K) => AccumuloKeyEncoder.encode(id, key, keyIndex.toIndex(key))

    logger.info(s"Saving updated RDD for layer ${id} to table $table")
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

      AccumuloRDDWriter.write(updated, instance, encodeKey, options.writeStrategy, table)
    } else {
      AccumuloRDDWriter.write(rdd, instance, encodeKey, options.writeStrategy, table)
    }
  }
}

object AccumuloLayerUpdater {
  def apply(instance: AccumuloInstance, options: Options = Options.DEFAULT)(implicit sc: SparkContext): AccumuloLayerUpdater =
    new AccumuloLayerUpdater(
      instance = instance,
      attributeStore = AccumuloAttributeStore(instance.connector),
      layerReader = AccumuloLayerReader(instance),
      options = options
    )
}
