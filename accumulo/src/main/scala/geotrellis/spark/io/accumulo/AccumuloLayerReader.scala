package geotrellis.spark.io.accumulo

import geotrellis.raster.{MultiBandTile, Tile}
import geotrellis.spark.io.avro._
import geotrellis.spark.io.avro.codecs._
import geotrellis.spark.io.json._
import geotrellis.spark._
import geotrellis.spark.io.index.KeyIndex
import geotrellis.spark.io._
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
    M: JsonFormat
  ](id: LayerId, rasterQuery: RDDQuery[K, M], numPartitions: Int) = {
    if (!attributeStore.layerExists(id)) throw new LayerNotFoundError(id)

    val (header, metaData, keyBounds, keyIndex, writerSchema) = try {
      attributeStore.readLayerAttributes[AccumuloLayerHeader, M, KeyBounds[K], KeyIndex[K], Schema](id)
    } catch {
      case e: AttributeNotFoundError => throw new LayerReadError(id).initCause(e)
    }

    val queryKeyBounds = rasterQuery(metaData, keyBounds)

    val decompose = (bounds: KeyBounds[K]) =>
      keyIndex.indexRanges(bounds).map { case (min, max) =>
        new AccumuloRange(new Text(AccumuloKeyEncoder.long2Bytes(min)), new Text(AccumuloKeyEncoder.long2Bytes(max)))
      }

    val rdd = AccumuloRDDReader.read[K, V](header.tileTable, columnFamily(id), queryKeyBounds, decompose, Some(writerSchema))
    new ContextRDD(rdd, metaData)
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
