package geotrellis.spark.io.cassandra

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.avro._
import geotrellis.util._

import org.apache.spark.SparkContext
import spray.json.JsonFormat

import scala.reflect.ClassTag

class CassandraLayerCopier(
  attributeStore: AttributeStore,
  layerReader: CassandraLayerReader,
  getLayerWriter: LayerId => CassandraLayerWriter
) extends LayerCopier[LayerId] {
  def copy[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]
  ](from: LayerId, to: LayerId): Unit = {
    if (!attributeStore.layerExists(from)) throw new LayerNotFoundError(from)
    if (attributeStore.layerExists(to)) throw new LayerExistsError(to)

    val keyIndex = try {
      attributeStore.readKeyIndex[K](from)
    } catch {
      case e: AttributeNotFoundError => throw new LayerCopyError(from, to).initCause(e)
    }

    try {
      getLayerWriter(from).write(to, layerReader.read[K, V, M](from), keyIndex)
    } catch {
      case e: Exception => new LayerCopyError(from, to).initCause(e)
    }
  }
}

object CassandraLayerCopier {
  def apply(
    attributeStore: AttributeStore,
    layerReader: CassandraLayerReader,
    getLayerWriter: LayerId => CassandraLayerWriter
  )(implicit sc: SparkContext): CassandraLayerCopier =
    new CassandraLayerCopier(
      attributeStore,
      layerReader,
      getLayerWriter
    )

  def apply(
    attributeStore: AttributeStore,
    layerReader: CassandraLayerReader,
    layerWriter: CassandraLayerWriter
  )(implicit sc: SparkContext): CassandraLayerCopier =
    apply(
      attributeStore,
      layerReader,
      _ => layerWriter
    )

  def apply(
    instance   : CassandraInstance,
    layerReader: CassandraLayerReader,
    layerWriter: CassandraLayerWriter
  )(implicit sc: SparkContext): CassandraLayerCopier =
    apply(
      CassandraAttributeStore(instance),
      layerReader,
      _ => layerWriter
    )

  def apply(
    instance: CassandraInstance,
    targetKeyspace: String,
    targetTable: String
  )(implicit sc: SparkContext): CassandraLayerCopier =
    apply(
      CassandraAttributeStore(instance),
      CassandraLayerReader(instance),
      _ => CassandraLayerWriter(instance, targetKeyspace, targetTable)
    )

  def apply(
    instance: CassandraInstance
  )(implicit sc: SparkContext): CassandraLayerCopier = {
    val attributeStore = CassandraAttributeStore(instance)
    apply(
      attributeStore,
      CassandraLayerReader(instance),
      { layerId: LayerId =>
        val header = attributeStore.readHeader[CassandraLayerHeader](layerId)
        CassandraLayerWriter(instance, header.keyspace, header.tileTable)
      }
    )
  }
}
