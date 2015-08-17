package geotrellis.spark.io.cassandra.spatial

import com.datastax.driver.core.ResultSet
import com.datastax.driver.core.querybuilder.QueryBuilder
import com.datastax.driver.core.querybuilder.QueryBuilder.{eq => eqs}
import geotrellis.spark._
import geotrellis.spark.io.cassandra._
import geotrellis.spark.io.index._
import geotrellis.spark.io.avro.KeyCodecs._

object SpatialTileReader extends TileReader[SpatialKey] {

  def collectTile(
    layerId: LayerId,
    kIndex: KeyIndex[SpatialKey],
    tileTable: String,
    key: SpatialKey
  )(implicit session: CassandraSession): ResultSet = {

    val indexer = kIndex.toIndex(key).toString
    val query = QueryBuilder.select("value").from(session.keySpace, tileTable)
      .where (eqs("reverse_index", indexer.reverse))
      .and   (eqs("zoom", layerId.zoom))
      .and   (eqs("indexer", indexer))
      .and   (eqs("name", layerId.name))
    
    session.execute(query)
  }
}
