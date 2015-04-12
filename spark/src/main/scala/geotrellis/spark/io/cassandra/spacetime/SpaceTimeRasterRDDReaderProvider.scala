package geotrellis.spark.io.cassandra.spacetime

import java.nio.ByteBuffer

import org.joda.time.DateTime

import scala.collection.mutable.ArrayBuffer
import scala.util.matching.Regex

import geotrellis.index.zcurve._
import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.cassandra._

import org.apache.hadoop.io.Text
import org.apache.hadoop.mapreduce.Job

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

import com.datastax.spark.connector.rdd.CassandraRDD
import com.datastax.spark.connector._

object SpaceTimeRasterRDDIndex {

  val rowIdRx = new Regex("""(\d+)_(\d+)""", "zoom", "zindex")

  def timeChunk(time: DateTime): String =
    time.getYear.toString

  def rowId(id: LayerId, zindex: Z3): String =
    f"${id.zoom}%02d_${zindex.z}%019d"

  def rowId(id: LayerId, key: SpaceTimeKey): String = {
    val SpaceTimeKey(SpatialKey(col, row), TemporalKey(time)) = key
    val t = timeChunk(time).toInt
    rowId(id, Z3(col, row, t))
  }
}

object SpaceTimeRasterRDDReaderProvider extends RasterRDDReaderProvider[SpaceTimeKey] {
  import SpaceTimeRasterRDDIndex._

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

  def reader(instance: CassandraInstance, metaData: CassandraLayerMetaData, keyBounds: KeyBounds[SpaceTimeKey])(implicit sc: SparkContext): FilterableRasterRDDReader[SpaceTimeKey] =
    new FilterableRasterRDDReader[SpaceTimeKey] {
      def read(layerId: LayerId, filters: FilterSet[SpaceTimeKey]): RasterRDD[SpaceTimeKey] = {
        val CassandraLayerMetaData(rasterMetaData, _, _, tileTable) = metaData

        val rdd: CassandraRDD[(String, ByteBuffer)] = 
          sc.cassandraTable[(String, ByteBuffer)](instance.keyspace, tileTable).select("id", "tile")

        val filteredRDD = applyFilter(rdd, layerId, filters)

        val tileRDD =
          filteredRDD.map { case (id, tilebytes) =>
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
    }
}
