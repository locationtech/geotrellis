package geotrellis.spark.io.cassandra

import java.nio.ByteBuffer;

import scala.collection.mutable.ArrayBuffer
import scala.util.matching.Regex

import geotrellis.raster.{Tile, ArrayTile, GridBounds}
import geotrellis.spark._

import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext

import com.datastax.spark.connector.rdd.CassandraRDD
import com.datastax.driver.core.querybuilder.QueryBuilder
import com.datastax.driver.core.querybuilder.QueryBuilder.{eq => eqs}
import com.datastax.driver.core.Session

import geotrellis.index.zcurve._

import org.joda.time.DateTime

object SpaceTimeRasterCassandraDriver extends CassandraDriver[SpaceTimeKey] {

  val rowIdRx = new Regex("""(\d+)_(\d+)""", "zoom", "zindex")

  def timeChunk(time: DateTime): Int =
    time.getYear

  def rowId(id: LayerId, key: SpaceTimeKey): String = {
    val SpaceTimeKey(SpatialKey(col, row), TemporalKey(time)) = key
    val zindex = Z3(col, row, timeChunk(time))
    f"${id.zoom}%02d_${zindex.z}%019d"
  }

  def loadTile(session: Session, keyspace: String)(id: LayerId, metaData: RasterMetaData, table: String, key: SpaceTimeKey): Tile = {
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

  def decode(rdd: RDD[(String, ByteBuffer)], rasterMetaData: RasterMetaData): RasterRDD[SpaceTimeKey] = {
    val tileRDD =
      rdd.map { case (id, tilebytes) =>
        val rowIdRx(zoom, zindex) = id
        val Z3(col, row, time) = new Z3(zindex.toLong)
        val bytes = new Array[Byte](tilebytes.capacity)
        tilebytes.get(bytes, 0, bytes.length)
        val tile = 
          ArrayTile.fromBytes(
            bytes,
            rasterMetaData.cellType, 
            rasterMetaData.tileLayout.tileCols,
            rasterMetaData.tileLayout.tileRows
          )

        SpaceTimeKey(col.toInt, row.toInt, new DateTime(time)) -> tile.asInstanceOf[Tile]
    }
    
    new RasterRDD(tileRDD, rasterMetaData)
  }

  def applyFilter(rdd: CassandraRDD[(String, ByteBuffer)], layerId: LayerId, filterSet: FilterSet[SpaceTimeKey]): RDD[(String, ByteBuffer)] = {

    var tileBoundSet = filterSet.isEmpty
    var spaceFilters: List[GridBounds] = Nil
    var timeFilters: List[(DateTime, DateTime)] = Nil
    var rdds = ArrayBuffer[CassandraRDD[(String, ByteBuffer)]]()

    filterSet.filters.foreach {
      case SpaceFilter(bounds) =>
        spaceFilters = bounds :: spaceFilters
      case TimeFilter(start, end) =>
        timeFilters = (start, end) :: timeFilters
    }

    for {
      (tileFrom, tileTo) <- tileKeys(spaceFilters)
      (timeFrom, timeTo) <- timeKeys(timeFilters)
    } yield {
      val min = rowId(layerId, SpaceTimeKey(tileFrom, timeFrom))
      val max = rowId(layerId, SpaceTimeKey(tileTo, timeTo))
      rdds += rdd.where("id >= ? AND id <= ?", min, max)
    }
    
    if (!tileBoundSet) {
      val zmin = f"${layerId.zoom}%02d"
      val zmax = f"${layerId.zoom+1}%02d"
      rdds += rdd.where("id >= ? AND id < ?", zmin, zmax)
    }
    
    rdd.context.union(rdds.toSeq).asInstanceOf[RDD[(String, ByteBuffer)]] // Coalesce afterwards?
  }

  def tileKeys(filters: List[GridBounds]): List[(SpatialKey, SpatialKey)] =
    filters match {
      case Nil =>
        List(SpatialKey(0,0) -> SpatialKey(Int.MaxValue - 1, Int.MaxValue - 1))
      case _ =>
        for {
          bounds <- filters
          row <- bounds.rowMin to bounds.rowMax
        } yield SpatialKey(bounds.colMin, row) -> SpatialKey(bounds.colMax, row)
    }
  
  def timeKeys(filters: List[(DateTime, DateTime)]): List[(TemporalKey, TemporalKey)] =
    filters match {
      case Nil =>
        List(TemporalKey(new DateTime(0)) -> TemporalKey(new DateTime().withYear(292278993)))
      case List((start, end)) =>
        List(TemporalKey(start) -> TemporalKey(end))
    }
    
}
