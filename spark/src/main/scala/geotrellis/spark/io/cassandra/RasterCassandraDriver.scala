package geotrellis.spark.io.cassandra

import java.nio.ByteBuffer;

import scala.collection.mutable.ArrayBuffer
import scala.util.matching.Regex

import geotrellis.raster.{Tile, ArrayTile}
import geotrellis.spark._

import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext

import com.datastax.spark.connector.rdd.CassandraRDD
import com.datastax.driver.core.querybuilder.QueryBuilder
import com.datastax.driver.core.querybuilder.QueryBuilder.{eq => eqs}
import com.datastax.driver.core.Session

import geotrellis.index.zcurve._

object RasterCassandraDriver extends CassandraDriver[SpatialKey] {

  val rowIdRx = new Regex("""(\d+)_(\d+)""", "zoom", "zindex")

  def rowId(id: LayerId, key: SpatialKey): String = {
    val SpatialKey(col, row) = key
    val zindex = Z2(col, row)
    f"${id.zoom}%02d_${zindex.z}%019d"
  }

  def loadTile(session: Session, keyspace: String)(id: LayerId, metaData: RasterMetaData, table: String, key: SpatialKey): Tile = {
    val query = QueryBuilder.select.column("tile").from(keyspace, table)
      .where (eqs("id", rowId(id, key)))
      .and   (eqs("name", id.name))

    val results = session.execute(query)

    val size = results.getAvailableWithoutFetching    
    val value = 
      if (size == 0) {
        sys.error(s"Tile with key $key not found for layer $id")
      } else if (size > 1) {
        sys.error(s"Multiple tiles found for $key for layer $id")
      } else {
        results.one.getBytes("tile")
      }

    val byteArray = new Array[Byte](value.remaining)
    value.get(byteArray, 0, byteArray.length)

    ArrayTile.fromBytes(
      byteArray,
      metaData.cellType,
      metaData.tileLayout.tileCols,
      metaData.tileLayout.tileRows
    )
  }

  def decode(rdd: RDD[(String, ByteBuffer)], rasterMetaData: RasterMetaData): RasterRDD[SpatialKey] = {
    val tileRDD =
      rdd.map { case (id, tilebytes) =>
        val rowIdRx(zoom, zindex) = id
        val Z2(col, row) = new Z2(zindex.toLong)
        val bytes = new Array[Byte](tilebytes.capacity)
        tilebytes.get(bytes, 0, bytes.length)
        val tile = 
          ArrayTile.fromBytes(
            bytes,
            rasterMetaData.cellType, 
            rasterMetaData.tileLayout.tileCols,
            rasterMetaData.tileLayout.tileRows
          )

        SpatialKey(col.toInt, row.toInt) -> tile.asInstanceOf[Tile]
    }
    
    new RasterRDD(tileRDD, rasterMetaData)
  }

  def applyFilter(rdd: CassandraRDD[(String, ByteBuffer)], layerId: LayerId, filterSet: FilterSet[SpatialKey]): RDD[(String, ByteBuffer)] = {
    var tileBoundSet = false
    var rdds = ArrayBuffer[CassandraRDD[(String, ByteBuffer)]]()

    for (filter <- filterSet.filters) {
      filter match {
        case SpaceFilter(bounds) =>
          tileBoundSet = true

          for(row <- bounds.rowMin to bounds.rowMax) {
            val min = rowId(layerId, SpatialKey(bounds.colMin, row))
            val max = rowId(layerId, SpatialKey(bounds.colMax, row))
            rdds += rdd.where("id >= ? AND id <= ?", min, max)
          }
      }
    }
    
    if (!tileBoundSet) {
      val zmin = f"${layerId.zoom}%02d"
      val zmax = f"${layerId.zoom+1}%02d"
      rdds += rdd.where("id >= ? AND id < ?", zmin, zmax)
    }
    
    rdd.context.union(rdds.toSeq).asInstanceOf[RDD[(String, ByteBuffer)]] // Coalesce afterwards?
  }
}
