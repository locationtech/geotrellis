package geotrellis.spark.io.cassandra

import geotrellis.spark._
import geotrellis.spark.io.index._
import geotrellis.raster._
import geotrellis.spark.utils._

import org.apache.spark.SparkContext

import scala.collection.JavaConversions._

import com.datastax.driver.core.ResultSet

import java.nio.ByteBuffer

trait TileReader[Key] {
  def collectTile(
    layerId: LayerId,
    kIndex: KeyIndex[Key],
    tileTable: String,
    key: Key
  )(implicit session: CassandraSession): ResultSet
    
  def read(
    layerId: LayerId, 
    cassandraLayerMetaData: CassandraLayerMetaData, 
    index: KeyIndex[Key]
  )(key: Key)(implicit session: CassandraSession): Tile = {

    val CassandraLayerMetaData(rasterMetaData, _, _, tileTable) = cassandraLayerMetaData
    val results = collectTile(layerId, index, tileTable, key)

    val size = results.getAvailableWithoutFetching
    val value = 
      if (size == 0) {
        sys.error(s"Tile with key $key not found for layer $layerId")
      } else if (size > 1) {
        sys.error(s"Multiple tiles found for $key for layer $layerId")
      } else {
        results.one.getBytes("value")
      }
    
    val byteArray = new Array[Byte](value.remaining)
    value.get(byteArray, 0, byteArray.length)
    
    val (_, tileBytes) = KryoSerializer.deserialize[(Key, Array[Byte])](ByteBuffer.wrap(byteArray))
    
    ArrayTile.fromBytes(
      tileBytes,
      rasterMetaData.cellType,
      rasterMetaData.tileLayout.tileCols,
      rasterMetaData.tileLayout.tileRows
    )
  }
}
