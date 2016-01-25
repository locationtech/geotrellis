package geotrellis.spark.io.accumulo

import geotrellis.raster.{MultibandTile, Tile}
import geotrellis.spark.io.json._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.avro.codecs._
import geotrellis.spark._
import geotrellis.spark.io.index.{KeyIndex, KeyIndexMethod}
import geotrellis.spark.io._

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import spray.json._
import scala.reflect._

class AccumuloLayerWriter[K: Boundable: JsonFormat: ClassTag, V: ClassTag, M: JsonFormat](
  val attributeStore: AttributeStore[JsonFormat],
  rddWriter: BaseAccumuloRDDWriter[K, V],
  table: String) extends Writer[LayerId, K, RDD[(K, V)] with Metadata[M]] {

  def write[I <: KeyIndex[K]: JsonFormat](id: LayerId, rdd: RDD[(K, V)] with Metadata[M], keyIndex: I): Unit = {
    val header =
      AccumuloLayerHeader(
        keyClass = classTag[K].toString(),
        valueClass = classTag[V].toString(),
        tileTable = table
      )
    val metadata = rdd.metadata
    val keyBounds = implicitly[Boundable[K]].getKeyBounds(rdd)
    val getRowId = (key: K) => index2RowId(keyIndex.toIndex(key))

    try {
      attributeStore.writeLayerAttributes(id, header, metadata, keyBounds, keyIndex, rddWriter.schema)
      rddWriter.write(rdd, table, columnFamily(id), getRowId, oneToOne = false)
    } catch {
      case e: Exception => throw new LayerWriteError(id).initCause(e)
    }
  }

  def write(id: LayerId, rdd: RDD[(K, V)] with Metadata[M], keyIndexMethod: KeyIndexMethod[K]): Unit = {
    val keyBounds = implicitly[Boundable[K]].getKeyBounds(rdd)
    write(id, rdd, keyIndexMethod.createIndex(keyBounds))
  }
}

object AccumuloLayerWriter {
  def defaultAccumuloWriteStrategy = HdfsWriteStrategy("/geotrellis-ingest")

  def apply[K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec: ClassTag, M: JsonFormat](
    instance: AccumuloInstance,
    table: String,
    strategy: AccumuloWriteStrategy = defaultAccumuloWriteStrategy
  ): AccumuloLayerWriter[K, V, M] =
    new AccumuloLayerWriter[K, V, M](
      attributeStore = AccumuloAttributeStore(instance.connector),
      rddWriter = new AccumuloRDDWriter[K, V](instance, strategy),
      table = table
    )

  def spatial(
    instance: AccumuloInstance, table: String,
    strategy: AccumuloWriteStrategy = defaultAccumuloWriteStrategy
  )(implicit sc: SparkContext) =
    apply[SpatialKey, Tile, RasterMetadata](instance, table, strategy)

  def spatialMultiband(
    instance: AccumuloInstance, table: String,
    strategy: AccumuloWriteStrategy = defaultAccumuloWriteStrategy
  )(implicit sc: SparkContext) =
    apply[SpatialKey, MultibandTile, RasterMetadata](instance, table, strategy)

  def spaceTime(
    instance: AccumuloInstance, table: String,
    strategy: AccumuloWriteStrategy = defaultAccumuloWriteStrategy
  )(implicit sc: SparkContext) =
    apply[SpaceTimeKey, Tile, RasterMetadata](instance, table, strategy)

  def spaceTimeMultiband(
    instance: AccumuloInstance,
    table: String,
    strategy: AccumuloWriteStrategy = defaultAccumuloWriteStrategy
  )(implicit sc: SparkContext) =
    apply[SpaceTimeKey, MultibandTile, RasterMetadata](instance, table, strategy)
}
