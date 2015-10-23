package geotrellis.spark.io.accumulo

import geotrellis.raster.mosaic.MergeView
import geotrellis.spark.io.json._
import geotrellis.spark.io.avro._
import geotrellis.spark._
import geotrellis.spark.io.index.{KeyIndex, KeyIndexMethod}
import geotrellis.spark.io._
import org.apache.avro.Schema
import org.apache.hadoop.io.Text
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import spray.json._
import org.apache.accumulo.core.data.{Range => AccumuloRange}
import geotrellis.spark.mosaic._
import scala.reflect._

class AccumuloLayerWriter[K: Boundable: JsonFormat: ClassTag, V: MergeView: ClassTag, Container](
    val attributeStore: AttributeStore[JsonFormat],
    rddWriter: BaseAccumuloRDDWriter[K, V],
    keyIndexMethod: KeyIndexMethod[K],
    table: String)
  (implicit val cons: ContainerConstructor[K, V, Container])
  extends Writer[LayerId, Container with RDD[(K, V)]] {

  def write(id: LayerId, rdd: Container with RDD[(K, V)]): Unit = {
    try {
      val header =
        AccumuloLayerHeader(
          keyClass = classTag[K].toString(),
          valueClass = classTag[V].toString(),
          tileTable = table
        )
      val metaData = cons.getMetaData(rdd)
      val keyBounds = implicitly[Boundable[K]].getKeyBounds(rdd.asInstanceOf[RDD[(K, V)]])
      val keyIndex = keyIndexMethod.createIndex(keyBounds)

      implicit val mdFormat = cons.metaDataFormat
      attributeStore.writeLayerAttributes(id, header, metaData, keyBounds, keyIndex, rddWriter.schema)

      val getRowId = (key: K) => index2RowId(keyIndex.toIndex(key))

      rddWriter.write(rdd, table, columnFamily(id), getRowId, oneToOne = false)
    } catch {
      case e: Exception => throw new LayerWriteError(id).initCause(e)
    }
  }

  def update(id: LayerId, rdd: Container with RDD[(K, V)], rddReader: BaseAccumuloRDDReader[K, V]) = {
    try {
      if(!attributeStore.layerExists(id)) throw new LayerNotExistsError(id)
      type MetaDataType = cons.MetaDataType
      implicit val mdFormat = cons.metaDataFormat
      implicit val sc = rdd.sparkContext
      val header =
        AccumuloLayerHeader(
          keyClass = classTag[K].toString(),
          valueClass = classTag[V].toString(),
          tileTable = table
        )
      val metaData = cons.getMetaData(rdd)
      val keyBounds = implicitly[Boundable[K]].getKeyBounds(rdd.asInstanceOf[RDD[(K, V)]])
      val keyIndex = keyIndexMethod.createIndex(keyBounds)

      val (existingHeader, existingMetaData, existingKeyBounds, existingKeyIndex, existingSchema) =
        attributeStore.readLayerAttributes[AccumuloLayerHeader, MetaDataType, KeyBounds[K], KeyIndex[K], Schema](id)

      val rasterQuery = new RDDQuery[K, MetaDataType].where(Intersects(keyBounds))
      val queryKeyBounds = rasterQuery(existingMetaData, existingKeyBounds)

      val decompose = (bounds: KeyBounds[K]) =>
        existingKeyIndex.indexRanges(bounds).map { case (min, max) =>
          new AccumuloRange(new Text(long2Bytes(min)), new Text(long2Bytes(max)))
        }

      val existing = rddReader.read(existingHeader.tileTable, columnFamily(id), queryKeyBounds, decompose, Some(existingSchema))

      val combinedMetaData = cons.combineMetaData(existingMetaData, metaData)
      val combinedKeyBounds = implicitly[Boundable[K]].combine(existingKeyBounds, keyBounds)
      val combinedRdd = existing merge rdd

      attributeStore.writeLayerAttributes(id, existingHeader, combinedMetaData, combinedKeyBounds, existingKeyIndex, existingSchema)

      val getRowId = (key: K) => index2RowId(existingKeyIndex.toIndex(key))

      rddWriter.write(combinedRdd, table, columnFamily(id), getRowId, oneToOne = false)
    } catch {
      case e: LayerNotExistsError => throw new LayerNotExistsError(id).initCause(e)
      case e: Exception => throw new LayerUpdateError(id).initCause(e)
    }
  }
}

object AccumuloLayerWriter {
  def defaultAccumuloWriteStrategy = HdfsWriteStrategy("/geotrellis-ingest")

  def apply[K: SpatialComponent: Boundable: AvroRecordCodec: JsonFormat: ClassTag,
            V: AvroRecordCodec: MergeView: ClassTag, Container[_]]
  (instance: AccumuloInstance,
   table: String,
   indexMethod: KeyIndexMethod[K],
   strategy: AccumuloWriteStrategy = defaultAccumuloWriteStrategy)
  (implicit cons: ContainerConstructor[K, V, Container[K]]): AccumuloLayerWriter[K, V, Container[K]] =
    new AccumuloLayerWriter(
      attributeStore = AccumuloAttributeStore(instance.connector),
      rddWriter = new AccumuloRDDWriter[K, V](instance, strategy),
      keyIndexMethod = indexMethod,
      table = table
    )
}