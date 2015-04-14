package geotrellis.spark.io.cassandra.spacetime

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.cassandra._
import geotrellis.spark.io.index._
import geotrellis.spark.utils._
import geotrellis.raster._

import com.datastax.driver.core.querybuilder.QueryBuilder
import com.datastax.driver.core.querybuilder.QueryBuilder.{eq => eqs}

import scala.collection.JavaConversions._

object SpaceTimeTileReaderProvider extends TileReaderProvider[SpaceTimeKey] {
  def index(tileLayout: TileLayout, keyBounds: KeyBounds[SpaceTimeKey]): KeyIndex[SpaceTimeKey] =
    ZSpaceTimeKeyIndex.byYear

  def reader(instance: CassandraInstance, layerId: LayerId, cassandraLayerMetaData: CassandraLayerMetaData, index: KeyIndex[SpaceTimeKey]): Reader[SpaceTimeKey, Tile] = {
    val CassandraLayerMetaData(rasterMetaData, _, _, tileTable) = cassandraLayerMetaData
    new Reader[SpaceTimeKey, Tile] {
      def read(key: SpaceTimeKey): Tile = {

        val i: Long = index.toIndex(key)
        val query = QueryBuilder.select.column("value").from(instance.keyspace, tileTable)
          .where (eqs("id", rowId(layerId, i)))
          .and   (eqs("name", layerId.name))
          .and   (eqs("time", timeText(key)))

        val results = instance.session.execute(query)

        val size = results.getAvailableWithoutFetching
        val value = 
          if (size == 0) {
            sys.error(s"Tile with key $key not found for layer $layerId")
          } else if (size > 1) {
            sys.error(s"Multiple tiles found for $key for layer $layerId")
          } else {
            results.one.getBytes("value")
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
