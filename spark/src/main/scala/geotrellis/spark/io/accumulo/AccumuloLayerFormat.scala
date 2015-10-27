package geotrellis.spark.io.accumulo

import geotrellis.raster.mosaic.MergeView
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.index.{KeyIndex, KeyIndexMethod}
import geotrellis.spark.io.json._
import geotrellis.spark.mosaic._
import org.apache.accumulo.core.data.{Range => AccumuloRange}
import org.apache.avro.Schema
import org.apache.hadoop.io.Text
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import spray.json._
import scala.reflect._

class AccumuloLayerFormat[K: Boundable: AvroRecordCodec: JsonFormat: ClassTag,
                          V: AvroRecordCodec: MergeView: ClassTag, Container]
(val attributeStore: AttributeStore[JsonFormat],
     keyIndexMethod: KeyIndexMethod[K],
     table         : String,
     rddWriter     : BaseAccumuloRDDWriter[K, V],
     rddReader     : BaseAccumuloRDDReader[K, V])
(implicit sc: SparkContext, val cons: ContainerConstructor[K, V, Container]) extends LayerFormat[LayerId, K, V, Container] {

  lazy val layerReader = new AccumuloLayerReader(attributeStore, rddReader)
  lazy val layerWriter = new AccumuloLayerWriter(attributeStore, rddWriter, keyIndexMethod, table)

  type MetaDataType = cons.MetaDataType

  val defaultNumPartitions = sc.defaultParallelism

  def update(id: LayerId, rdd: Container with RDD[(K, V)], numPartitions: Int) = {
    try {
      if(!attributeStore.layerExists(id)) throw new LayerNotExistsError(id)
      implicit val mdFormat = cons.metaDataFormat
      val header =
        AccumuloLayerHeader(
          keyClass = classTag[K].toString(),
          valueClass = classTag[V].toString(),
          tileTable = table
        )

      val (existingHeader, existingMetaData, existingKeyBounds, existingKeyIndex, existingSchema) =
        attributeStore.readLayerAttributes[AccumuloLayerHeader, MetaDataType, KeyBounds[K], KeyIndex[K], Schema](id)

      if(header != existingHeader) throw new HeaderMatchError(id, existingHeader, header)

      val metaData = cons.getMetaData(rdd)
      val keyBounds = implicitly[Boundable[K]].getKeyBounds(rdd.asInstanceOf[RDD[(K, V)]])

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
      case e: HeaderMatchError[_] => throw e.initCause(e)
      case e: LayerNotExistsError => throw e.initCause(e)
      case e: Exception => throw new LayerUpdateError(id).initCause(e)
    }
  }
}

object AccumuloLayerFormat {
  def defaultAccumuloWriteStrategy = HdfsWriteStrategy("/geotrellis-ingest")

  def apply[K: Boundable: AvroRecordCodec: JsonFormat: ClassTag,
            V: AvroRecordCodec: MergeView: ClassTag, Container[_]]
  (instance: AccumuloInstance,
   table: String,
   indexMethod: KeyIndexMethod[K],
   strategy: AccumuloWriteStrategy = defaultAccumuloWriteStrategy)
  (implicit sc: SparkContext, cons: ContainerConstructor[K, V, Container[K]]): AccumuloLayerFormat[K, V, Container[K]] =
    new AccumuloLayerFormat(
      attributeStore = AccumuloAttributeStore(instance.connector),
      keyIndexMethod = indexMethod,
      table = table,
      rddWriter = new AccumuloRDDWriter[K, V](instance, strategy),
      rddReader = new AccumuloRDDReader[K, V](instance)
    )
}
