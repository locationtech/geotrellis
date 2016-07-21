package geotrellis.spark.io.hbase

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.avro._
import geotrellis.util._

import org.apache.spark.SparkContext
import spray.json._

import scala.reflect._

class HBaseLayerReader(val attributeStore: AttributeStore, instance: HBaseInstance)(implicit sc: SparkContext)
  extends FilteringLayerReader[LayerId] {

  val defaultNumPartitions = sc.defaultParallelism

  def read[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]
  ](id: LayerId, rasterQuery: LayerQuery[K, M], numPartitions: Int, filterIndexOnly: Boolean) = {
    if (!attributeStore.layerExists(id)) throw new LayerNotFoundError(id)

    val LayerAttributes(header, metadata, keyIndex, writerSchema) = try {
      attributeStore.readLayerAttributes[HBaseLayerHeader, M, K](id)
    } catch {
      case e: AttributeNotFoundError => throw new LayerReadError(id).initCause(e)
    }

    val queryKeyBounds = rasterQuery(metadata)

    val decompose = (bounds: KeyBounds[K]) => keyIndex.indexRanges(bounds)

    val rdd = HBaseRDDReader.read[K, V](instance, header.tileTable, id, queryKeyBounds, decompose, filterIndexOnly, Some(writerSchema))
    new ContextRDD(rdd, metadata)
  }
}

object HBaseLayerReader {
  def apply(instance: HBaseInstance)(implicit sc: SparkContext): HBaseLayerReader =
    new HBaseLayerReader(HBaseAttributeStore(instance), instance)(sc)

  def apply(attributeStore: HBaseAttributeStore)(implicit sc: SparkContext): HBaseLayerReader =
    new HBaseLayerReader(attributeStore, attributeStore.instance)
}

