package geotrellis.spark.io.accumulo

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.index._
import geotrellis.raster._
import geotrellis.spark.utils._
import geotrellis.spark.io.accumulo._


import org.apache.spark.SparkContext
import org.apache.hadoop.io.Text
import org.apache.accumulo.core.security.Authorizations
import org.apache.accumulo.core.data.{Range => ARange, Value}

import scala.collection.JavaConversions._


trait TileReader[K, T] {
  def collectTile(
    instance: AccumuloInstance,
    layerId: LayerId,
    kIndex: KeyIndex[K],
    tileTable: String,
    key: K
  ): List[Value]

  def read(
    instance: AccumuloInstance,
    layerId: LayerId,
    accumuloLayerMetaData: AccumuloLayerMetaData,
    index: KeyIndex[K]
  )(key: K): T = {
    val AccumuloLayerMetaData(_, rasterMetaData, tileTable) = accumuloLayerMetaData
    val values = collectTile(instance, layerId, index, tileTable, key)
    val value =
      if(values.size == 0) {
        throw new TileNotFoundError(key, layerId)        
      } else if(values.size > 1) {
        throw new CatalogError(s"Multiple tiles found for $key for layer $layerId")
      } else {
        values.head
      }

    val (_, tile) = KryoSerializer.deserialize[(K, T)](value.get)
    tile
  }
}
