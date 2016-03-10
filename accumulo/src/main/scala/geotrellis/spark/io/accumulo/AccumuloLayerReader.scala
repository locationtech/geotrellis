package geotrellis.spark.io.accumulo

import geotrellis.raster.{MultibandTile, Tile}
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.index.KeyIndex

import org.apache.avro.Schema
import org.apache.hadoop.io.Text
import org.apache.spark.SparkContext
import org.apache.accumulo.core.data.{Range => AccumuloRange}
import org.apache.spark.rdd.RDD
import spray.json._

import scala.reflect._

class AccumuloLayerReader(val attributeStore: AttributeStore[JsonFormat])(implicit sc: SparkContext, instance: AccumuloInstance)
    extends FilteringLayerReader[LayerId] {

  val defaultNumPartitions = sc.defaultParallelism

  def read[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: Component[?, Bounds[K]]
  ](id: LayerId, rasterQuery: RDDQuery[K, M], numPartitions: Int, filterIndexOnly: Boolean) = {
    if (!attributeStore.layerExists(id)) throw new LayerNotFoundError(id)

    val (header, metadata, keyIndex, writerSchema) = try {
      attributeStore.readLayerAttributes[AccumuloLayerHeader, M, KeyIndex[K], Schema](id)
    } catch {
      case e: AttributeNotFoundError => throw new LayerReadError(id).initCause(e)
    }

    val queryKeyBounds = rasterQuery(metadata)

    val decompose = (bounds: KeyBounds[K]) =>
      keyIndex.indexRanges(bounds).map { case (min, max) =>
        new AccumuloRange(new Text(AccumuloKeyEncoder.long2Bytes(min)), new Text(AccumuloKeyEncoder.long2Bytes(max)))
      }

    val rdd = AccumuloRDDReader.read[K, V](header.tileTable, columnFamily(id), queryKeyBounds, decompose, filterIndexOnly, Some(writerSchema))
    new ContextRDD(rdd, metadata)
  }
}

object AccumuloLayerReader {
  def apply(instance: AccumuloInstance)(implicit sc: SparkContext): AccumuloLayerReader =
    new AccumuloLayerReader(AccumuloAttributeStore(instance.connector))(sc, instance)

  def apply(attributeStore: AccumuloAttributeStore)(implicit sc: SparkContext, instance: AccumuloInstance): AccumuloLayerReader =
    new AccumuloLayerReader(attributeStore)

  def apply()(implicit sc: SparkContext, instance: AccumuloInstance): AccumuloLayerReader =
    new AccumuloLayerReader(AccumuloAttributeStore(instance.connector))
}
