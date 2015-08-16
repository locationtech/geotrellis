package geotrellis.spark.io.accumulo

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.avro.{AvroRecordCodec, TupleCodec, AvroEncoder}
import geotrellis.spark.io.index._
import geotrellis.raster._
import geotrellis.spark.utils._

import org.apache.accumulo.core.data.Value

abstract class TileReader[K: AvroRecordCodec] {

  private lazy val readCodec = KryoWrapper(TupleCodec[K, Tile])

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
  )(key: K): Tile = {
    val AccumuloLayerMetaData(_, _, tileTable) = accumuloLayerMetaData
    val values = collectTile(instance, layerId, index, tileTable, key)
    val value =
      if(values.isEmpty) {
        throw new TileNotFoundError(key, layerId)        
      } else if(values.size > 1) {
        throw new CatalogError(s"Multiple tiles found for $key for layer $layerId")
      } else {
        values.head
      }

    val (_, tile) = AvroEncoder.fromBinary(value.get)(readCodec.value)
    tile
  }
}
