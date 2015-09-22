package geotrellis.spark.io.accumulo

import geotrellis.spark.io.AttributeStore.Fields
import geotrellis.spark.io.json._
import geotrellis.spark._
import geotrellis.spark.io.index.KeyIndexMethod
import geotrellis.spark.io.{AttributeStore, ContainerConstructor, Writer}
import org.apache.spark.rdd.RDD

import org.apache.hadoop.io.Text
import spray.json._
import spray.json.DefaultJsonProtocol._

import scala.reflect._

class AccumuloLayerWriter[K: SpatialComponent: Boundable: JsonFormat: ClassTag, TileType: ClassTag, Container[_]](
    val attributeStore: AttributeStore.Aux[JsonFormat],
    rddWriter: IAccumuloRDDWriter[K, TileType],
    keyIndexMethod: KeyIndexMethod[K],
    table: String)
  (implicit val cons: ContainerConstructor[K, TileType, Container])
  extends Writer[LayerId, Container[K] with RDD[(K, TileType)]] {

  def write(id: LayerId, rdd: Container[K] with RDD[(K, TileType)]): Unit = {
    val layerMetaData =
      AccumuloLayerMetaData(
        keyClass = classTag[K].toString(),
        valueClass = classTag[TileType].toString(),
        tileTable = table
      )
    val rasterMetaData = cons.getMetaData(rdd)
    val keyBounds = implicitly[Boundable[K]].getKeyBounds(rdd.asInstanceOf[RDD[(K, TileType)]])
    // TODO: do we need to expand the tileLayout here ?
    val keyIndex = keyIndexMethod.createIndex(keyBounds)

    attributeStore.cacheWrite(id, Fields.layerMetaData, layerMetaData)
    attributeStore.cacheWrite(id, Fields.layerMetaData, rasterMetaData)(cons.metaDataFormat)
    attributeStore.cacheWrite(id, Fields.keyBounds, keyBounds)
    attributeStore.cacheWrite(id, Fields.keyIndex, keyIndex)
    attributeStore.cacheWrite(id, Fields.schema, rddWriter.schema.toString.parseJson)

    val getRowId = (key: K) =>
      new Text(f"${id.zoom}%02d_${keyIndex.toIndex(key)}%06d")

    rddWriter.write(rdd, table, id.name, getRowId, oneToOne = false)

  }
}
