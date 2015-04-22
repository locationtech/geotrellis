package geotrellis.spark.io.accumulo

import geotrellis.spark._
import geotrellis.spark.io.index._
import geotrellis.raster._
import geotrellis.spark.utils._
import geotrellis.spark.io.accumulo._


import org.apache.spark.SparkContext
import org.apache.hadoop.io.Text
import org.apache.accumulo.core.security.Authorizations
import org.apache.accumulo.core.data.{Range => ARange, Key => AccumuloKey, Value => AccumuloValue}

import scala.collection.JavaConversions._


trait TileReader[Key] {
  def collectTile(
    instance: AccumuloInstance,
    layerId: LayerId,
    kIndex: KeyIndex[Key],
    tileTable: String,
    key: Key
  ): List[AccumuloValue]

  def read(
    instance: AccumuloInstance,
    layerId: LayerId,
    accumuloLayerMetaData: AccumuloLayerMetaData,
    index: KeyIndex[Key]
  )(key: Key): Tile = {
    val AccumuloLayerMetaData(rasterMetaData, _, _, tileTable) = accumuloLayerMetaData
    val values = collectTile(instance, layerId, index, tileTable, key)
    val value =
      if(values.size == 0) {
        sys.error(s"Tile with key $key not found for layer $layerId")
      } else if(values.size > 1) {
        sys.error(s"Multiple tiles found for $key for layer $layerId")
      } else {
        values.head
      }

    val (_, tileBytes) = KryoSerializer.deserialize[(Key, Array[Byte])](value.get)

    ArrayTile.fromBytes(
      tileBytes,
      rasterMetaData.cellType,
      rasterMetaData.tileLayout.tileCols,
      rasterMetaData.tileLayout.tileRows
    )
  }
}
