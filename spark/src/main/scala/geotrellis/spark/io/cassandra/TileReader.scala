package geotrellis.spark.io.cassandra

import com.datastax.driver.core.ResultSet
import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.avro.{AvroEncoder, AvroRecordCodec, TupleCodec}
import geotrellis.spark.io.index._
import geotrellis.spark.utils._

abstract class TileReader[K: AvroRecordCodec] {

  def collectTile(
    layerId: LayerId,
    kIndex: KeyIndex[K],
    tileTable: String,
    key: K
  )(implicit session: CassandraSession): ResultSet

  def read(
    layerId: LayerId,
    cassandraLayerMetaData: CassandraLayerMetaData,
    index: KeyIndex[K]
  )(key: K)(implicit session: CassandraSession): Tile = {

    val readCodec = KryoWrapper(TupleCodec[K, Tile])
    val CassandraLayerMetaData(_, rasterMetaData, tileTable) = cassandraLayerMetaData
    val results = collectTile(layerId, index, tileTable, key)

    val size = results.getAvailableWithoutFetching
    val value =
      if (size == 0) {
        throw new TileNotFoundError(key, layerId)
      } else if (size > 1) {
        throw new CatalogError(s"Multiple tiles found for $key for layer $layerId")
      } else {
        results.one.getBytes("value")
      }

    val byteArray = new Array[Byte](value.remaining)
    value.get(byteArray, 0, byteArray.length)

    val (_, tile) = AvroEncoder.fromBinary(byteArray)(readCodec.value)
    tile
  }
}
