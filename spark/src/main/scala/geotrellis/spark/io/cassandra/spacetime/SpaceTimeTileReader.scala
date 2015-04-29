package geotrellis.spark.io.cassandra.spacetime

import geotrellis.spark._
import geotrellis.spark.io.cassandra._
import geotrellis.spark.io.index._
import geotrellis.spark.utils._
import geotrellis.raster._

import com.datastax.driver.core.ResultSet
import com.datastax.driver.core.querybuilder.QueryBuilder
import com.datastax.driver.core.querybuilder.QueryBuilder.{eq => eqs}

import scala.collection.JavaConversions._

import java.nio.ByteBuffer

object SpaceTimeTileReader extends TileReader[SpaceTimeKey] {
  def collectTile(
    layerId: LayerId,
    kIndex: KeyIndex[SpaceTimeKey],
    tileTable: String,
    key: SpaceTimeKey
  )(implicit session: CassandraSession): ResultSet = {
    val i = kIndex.toIndex(key).toString
    val query = QueryBuilder.select.column("value").from(session.keySpace, tileTable)
      .where (eqs("reverse_index", i.reverse))
      .and   (eqs("zoom", layerId.zoom))
      .and   (eqs("indexer", i))
      .and   (eqs("date", timeText(key)))
      .and   (eqs("name", layerId.name))

    session.execute(query)
  }
}
