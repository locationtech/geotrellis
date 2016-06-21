package geotrellis.spark.io.accumulo

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.avro._
import geotrellis.util._

import org.apache.hadoop.io.Text
import org.apache.spark.SparkContext
import org.apache.accumulo.core.data.{Range => AccumuloRange}
import spray.json._

import scala.reflect._

class AccumuloLayerReader(val attributeStore: AttributeStore)(implicit sc: SparkContext, instance: AccumuloInstance)
    extends FilteringLayerReader[LayerId] {

  val defaultNumPartitions = sc.defaultParallelism

  def read[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]
  ](id: LayerId, rasterQuery: LayerQuery[K, M], numPartitions: Int, filterIndexOnly: Boolean) = {
    if (!attributeStore.layerExists(id)) throw new LayerNotFoundError(id)

    val LayerAttributes(header, metadata, keyIndex, writerSchema) = try {
      attributeStore.readLayerAttributes[AccumuloLayerHeader, M, K](id)
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
