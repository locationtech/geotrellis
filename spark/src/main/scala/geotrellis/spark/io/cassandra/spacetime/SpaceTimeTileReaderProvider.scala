package geotrellis.spark.io.cassandra.spacetime

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.cassandra._
import geotrellis.raster._

import com.datastax.driver.core.querybuilder.QueryBuilder
import com.datastax.driver.core.querybuilder.QueryBuilder.{eq => eqs}

import scala.collection.JavaConversions._

object SpaceTimeTileReaderProvider extends TileReaderProvider[SpaceTimeKey] {
  import SpaceTimeRasterRDDIndex._

  def reader(instance: CassandraInstance, layerId: LayerId, cassandraLayerMetaData: CassandraLayerMetaData): Reader[SpaceTimeKey, Tile] = {
    val CassandraLayerMetaData(rasterMetaData, _, _, tileTable) = cassandraLayerMetaData
    new Reader[SpaceTimeKey, Tile] {
      def read(key: SpaceTimeKey): Tile = {

        val query = QueryBuilder.select.column("tile").from(instance.keyspace, tileTable)
          .where (eqs("id", rowId(layerId, key)))
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
        
        val byteArray = new Array[Byte](value.remaining)
        value.get(byteArray, 0, byteArray.length)

        ArrayTile.fromBytes(
          byteArray,
          rasterMetaData.cellType,
          rasterMetaData.tileLayout.tileCols,
          rasterMetaData.tileLayout.tileRows
        )
      }
    }
  }

}
