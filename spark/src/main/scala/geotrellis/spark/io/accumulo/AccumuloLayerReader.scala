package geotrellis.spark.io.accumulo

import geotrellis.raster.{MultibandTile, Tile}
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

class AccumuloLayerReader[
  K: Boundable: AvroRecordCodec: JsonFormat: ClassTag,
  V: AvroRecordCodec: ClassTag,
  M: JsonFormat
](val attributeStore: AttributeStore[JsonFormat], rddReader: BaseAccumuloRDDReader[K, V])
 (implicit sc: SparkContext) extends FilteringLayerReader[LayerId, K, M, RDD[(K, V)] with Metadata[M]] {

  val defaultNumPartitions = sc.defaultParallelism

  def read[I <: KeyIndex[K]: JsonFormat](id: LayerId, rasterQuery: RDDQuery[K, M], numPartitions: Int, format: JsonFormat[I]): RDD[(K, V)] with Metadata[M] = {
    if (!attributeStore.layerExists(id)) throw new LayerNotFoundError(id)

    val (header, metadata, keyBounds, keyIndex, writerSchema) = try {
      attributeStore.readLayerAttributes[AccumuloLayerHeader, M, KeyBounds[K], I, Schema](id)
    } catch {
      case e: AttributeNotFoundError => throw new LayerReadError(id).initCause(e)
    }

    val queryKeyBounds = rasterQuery(metadata, keyBounds)

    val decompose = (bounds: KeyBounds[K]) =>
      keyIndex.indexRanges(bounds).map { case (min, max) =>
        new AccumuloRange(new Text(long2Bytes(min)), new Text(long2Bytes(max)))
      }

    val rdd = rddReader.read(header.tileTable, columnFamily(id), queryKeyBounds, decompose, Some(writerSchema))
    new ContextRDD(rdd, metadata)
  }
}

object AccumuloLayerReader {
  def apply[
    K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, 
    V: AvroRecordCodec: ClassTag, 
    M: JsonFormat
  ](instance: AccumuloInstance)(implicit sc: SparkContext): AccumuloLayerReader[K, V, M] =
    new AccumuloLayerReader[K, V, M] (
      AccumuloAttributeStore(instance.connector),
      new AccumuloRDDReader[K, V](instance))

  def spatial(instance: AccumuloInstance)(implicit sc: SparkContext) =
    apply[SpatialKey, Tile, RasterMetadata](instance)

  def spatialMultiband(instance: AccumuloInstance)(implicit sc: SparkContext) =
    apply[SpatialKey, MultibandTile, RasterMetadata](instance)

  def spaceTime(instance: AccumuloInstance)(implicit sc: SparkContext) =
    apply[SpaceTimeKey, Tile, RasterMetadata](instance)

  def spaceTimeMultiband(instance: AccumuloInstance)(implicit sc: SparkContext) =
    apply[SpaceTimeKey, MultibandTile, RasterMetadata](instance)
}
