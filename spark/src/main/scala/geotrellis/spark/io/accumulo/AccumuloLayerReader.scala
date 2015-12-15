package geotrellis.spark.io.accumulo

import geotrellis.raster.{MultiBandTile, Tile}
import geotrellis.spark.io.accumulo.AccumuloRDDReader
import geotrellis.spark.io.avro._
import geotrellis.spark.io.hadoop.{HadoopCatalogConfig, HadoopRDDReader, HadoopAttributeStore}
import geotrellis.spark.io.json._
import geotrellis.spark._
import geotrellis.spark.io.index.KeyIndex
import geotrellis.spark.io._
import org.apache.avro.Schema
import org.apache.hadoop.fs.Path
import org.apache.hadoop.io.Text
import org.apache.spark.SparkContext
import org.apache.accumulo.core.data.{Range => AccumuloRange}
import org.apache.spark.rdd.RDD
import spray.json._
import spray.json.DefaultJsonProtocol._
import scala.reflect._

class AccumuloLayerReader[K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec: ClassTag, M: JsonFormat, C <: RDD[(K, V)]](
    val attributeStore: AttributeStore[JsonFormat],
    rddReader: BaseAccumuloRDDReader[K, V])
  (implicit sc: SparkContext, val cons: ContainerConstructor[K, V, M, C])
  extends FilteringLayerReader[LayerId, K, M, C] {

  val defaultNumPartitions = sc.defaultParallelism

  def read(id: LayerId, rasterQuery: RDDQuery[K, M], numPartitions: Int) = {
    if (!attributeStore.layerExists(id)) throw new LayerNotFoundError(id)

    val (header, metaData, keyBounds, keyIndex, writerSchema) = try {
      attributeStore.readLayerAttributes[AccumuloLayerHeader, M, KeyBounds[K], KeyIndex[K], Schema](id)
    } catch {
      case e: AttributeNotFoundError => throw new LayerReadError(id).initCause(e)
    }

    val queryKeyBounds = rasterQuery(metaData, keyBounds)

    val decompose = (bounds: KeyBounds[K]) =>
      keyIndex.indexRanges(bounds).map { case (min, max) =>
        new AccumuloRange(new Text(long2Bytes(min)), new Text(long2Bytes(max)))
      }

    val rdd = rddReader.read(header.tileTable, columnFamily(id), queryKeyBounds, decompose, Some(writerSchema))
    cons.makeContainer(rdd, keyBounds, metaData)
  }
}

object AccumuloLayerReader {
  def apply[K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec: ClassTag, M: JsonFormat, C <: RDD[(K, V)]](instance: AccumuloInstance)
    (implicit sc: SparkContext, cons: ContainerConstructor[K, V, M, C]): AccumuloLayerReader[K, V, M, C] =
    new AccumuloLayerReader[K, V, M, C] (
      AccumuloAttributeStore(instance.connector),
      new AccumuloRDDReader[K, V](instance))

  def spatial(instance: AccumuloInstance)
    (implicit sc: SparkContext, cons: ContainerConstructor[SpatialKey, Tile, RasterMetaData, RasterRDD[SpatialKey]]) =
    new AccumuloLayerReader(AccumuloAttributeStore(instance.connector), new AccumuloRDDReader[SpatialKey, Tile](instance))

  def spatialMultiBand((instance: AccumuloInstance)
    (implicit sc: SparkContext, cons: ContainerConstructor[SpatialKey, MultiBandTile, RasterMetaData, MultiBandRasterRDD[SpatialKey]]) =
    new AccumuloLayerReader(AccumuloAttributeStore(instance.connector), new AccumuloRDDReader[SpatialKey, MultiBandTile](instance))

}
