package geotrellis.spark.io.cassandra.spatial

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.cassandra._
import geotrellis.spark.io.index._
import geotrellis.spark.utils._
import geotrellis.raster._

import com.datastax.driver.core.querybuilder.QueryBuilder
import com.datastax.driver.core.querybuilder.QueryBuilder.{eq => eqs}

import scala.collection.JavaConversions._

object SpatialTileReaderProvider extends TileReaderProvider[SpatialKey] {

  def reader(instance: CassandraInstance, layerId: LayerId, cassandraLayerMetaData: CassandraLayerMetaData, index: KeyIndex[SpatialKey]): Reader[SpatialKey, Tile] = {
    val CassandraLayerMetaData(rasterMetaData, _, _, tileTable) = cassandraLayerMetaData
    new Reader[SpatialKey, Tile] {
      def read(key: SpatialKey): Tile = {

        val query = QueryBuilder.select.column("value").from(instance.keyspace, tileTable)
          .where (eqs("id", rowId(layerId, index.toIndex(key))))
          .and   (eqs("name", layerId.name))

        val results = instance.session.execute(query)

        val size = results.getAvailableWithoutFetching
        val value = 
          if (size == 0) {
            sys.error(s"Tile with key $key not found for layer $layerId")
          } else if (size > 1) {
            sys.error(s"Multiple tiles found for $key for layer $layerId")
          } else {
            results.one.getBytes("tile")
          }
        
        val (_, tileBytes) = KryoSerializer.deserialize[(SpaceTimeKey, Array[Byte])](value)

        ArrayTile.fromBytes(
          tileBytes,
          rasterMetaData.cellType,
          rasterMetaData.tileLayout.tileCols,
          rasterMetaData.tileLayout.tileRows
        )
      }
    }
  }

}
