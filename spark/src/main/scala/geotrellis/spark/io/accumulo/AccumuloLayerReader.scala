package geotrellis.spark.io.accumulo


import geotrellis.spark.io.AttributeStore.Fields
import geotrellis.spark.io.avro.AvroRecordCodec
import geotrellis.spark.io.avro.codecs.KeyValueRecordCodec
import geotrellis.spark.io.json._
import geotrellis.spark._
import geotrellis.spark.io.index.KeyIndex
import geotrellis.spark.io._
import geotrellis.spark.utils.KryoWrapper
import org.apache.avro.Schema
import org.apache.spark.SparkContext

import org.apache.hadoop.io.Text
import spray.json._
import spray.json.DefaultJsonProtocol._

import scala.reflect._

class AccumuloLayerReader[K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, TileType: AvroRecordCodec: ClassTag, Container[_]](
    val attributeStore: AttributeStore.Aux[JsonFormat],
    rddReader: AccumuloRDDReader[K, TileType])
  (implicit sc: SparkContext, val cons: ContainerConstructor[K, TileType, Container])
  extends FilteringRasterRDDReader[K, Container[K]] {

  type MetaDataType = cons.MetaDataType

  val defaultNumPartitions = sc.defaultParallelism

  def read(id: LayerId, rasterQuery: RDDQuery[K, MetaDataType], numPartitions: Int) = {
    val layerMetaData  = attributeStore.cacheRead[AccumuloLayerMetaData](id, Fields.layerMetaData)
    val metadata  = attributeStore.cacheRead[MetaDataType](id, Fields.rddMetadata)(cons.metaDataFormat)
    val keyBounds = attributeStore.cacheRead[KeyBounds[K]](id, Fields.keyBounds)
    val keyIndex  = attributeStore.cacheRead[KeyIndex[K]](id, Fields.keyIndex)
    val queryKeyBounds = rasterQuery(metadata, keyBounds)
    val writerSchema: Schema = (new Schema.Parser)
      .parse(attributeStore.cacheRead[JsObject](id, Fields.schema).toString())

    val getRow = (index: Long) =>
      new Text(f"${id.zoom}%02d_$index%06d")

    val rdd = rddReader.read(layerMetaData.tileTable, id.name, getRow, keyIndex, queryKeyBounds, writerSchema)

    cons.makeContainer(rdd, keyBounds, metadata)
  }
}
