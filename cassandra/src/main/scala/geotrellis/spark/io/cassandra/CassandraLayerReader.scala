package geotrellis.spark.io.cassandra

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.avro._
import geotrellis.util._

import org.apache.spark.SparkContext
import spray.json._

import scala.reflect._

class CassandraLayerReader(val attributeStore: AttributeStore, instance: CassandraInstance)(implicit sc: SparkContext)
  extends FilteringLayerReader[LayerId] {

  val defaultNumPartitions = sc.defaultParallelism

  def read[
  K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
  V: AvroRecordCodec: ClassTag,
  M: JsonFormat: GetComponent[?, Bounds[K]]
  ](id: LayerId, rasterQuery: LayerQuery[K, M], numPartitions: Int, filterIndexOnly: Boolean) = {
    if (!attributeStore.layerExists(id)) throw new LayerNotFoundError(id)

    val LayerAttributes(header, metadata, keyIndex, writerSchema) = try {
      attributeStore.readLayerAttributes[CassandraLayerHeader, M, K](id)
    } catch {
      case e: AttributeNotFoundError => throw new LayerReadError(id).initCause(e)
    }

    val queryKeyBounds = rasterQuery(metadata)

    val decompose = (bounds: KeyBounds[K]) => keyIndex.indexRanges(bounds)

    val rdd = CassandraRDDReader.read[K, V](instance, header.keyspace, header.tileTable, id, queryKeyBounds, decompose, filterIndexOnly, Some(writerSchema))
    new ContextRDD(rdd, metadata)
  }
}

object CassandraLayerReader {
  def apply(instance: CassandraInstance)(implicit sc: SparkContext): CassandraLayerReader =
    new CassandraLayerReader(CassandraAttributeStore(instance), instance)(sc)

  def apply(attributeStore: CassandraAttributeStore)(implicit sc: SparkContext): CassandraLayerReader =
    new CassandraLayerReader(attributeStore, attributeStore.instance)
}

