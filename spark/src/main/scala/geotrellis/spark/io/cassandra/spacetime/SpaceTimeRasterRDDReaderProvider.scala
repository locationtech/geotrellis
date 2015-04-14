package geotrellis.spark.io.cassandra.spacetime

import java.nio.ByteBuffer

import org.joda.time.DateTime

import scala.collection.mutable
import scala.util.matching.Regex

import geotrellis.index.zcurve._
import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.cassandra._
import geotrellis.spark.io.json._
import geotrellis.spark.io.index._
import geotrellis.spark.utils._

import org.apache.hadoop.io.Text
import org.apache.hadoop.mapreduce.Job

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

import com.datastax.spark.connector.rdd.CassandraRDD
import com.datastax.spark.connector._

object SpaceTimeRasterRDDReaderProvider extends RasterRDDReaderProvider[SpaceTimeKey] {
  def index(tileLayout: TileLayout, keyBounds: KeyBounds[SpaceTimeKey]): KeyIndex[SpaceTimeKey] =
    ZSpaceTimeKeyIndex.byYear

  def tileSlugs(filters: List[GridBounds]): List[(String, String)] = filters match {
    case Nil =>
      List(("0"*6 + "_" + "0"*6) -> ("9"*6 + "_" + "9"*6))
    case _ => 
      for{
        bounds <- filters
        row <- bounds.rowMin to bounds.rowMax 
      } yield f"${bounds.colMin}%06d_${row}%06d" -> f"${bounds.colMax}%06d_${row}%06d"
  }
  
  def timeSlugs(filters: List[(DateTime, DateTime)], minTime: DateTime, maxTime: DateTime): List[(Int, Int)] = filters match {
    case Nil =>
      List(timeChunk(minTime).toInt -> timeChunk(maxTime).toInt)
    case List((start, end)) =>                 
      List(timeChunk(start).toInt -> timeChunk(end).toInt)
  }

  def applyFilter(rdd: CassandraRDD[(String, ByteBuffer)], layerId: LayerId, filterSet: FilterSet[SpaceTimeKey], keyBounds: KeyBounds[SpaceTimeKey], index: KeyIndex[SpaceTimeKey]): RDD[(String, ByteBuffer)] = {
    val spaceFilters = mutable.ListBuffer[GridBounds]()
    val timeFilters = mutable.ListBuffer[(DateTime, DateTime)]()

    filterSet.filters.foreach {
      case SpaceFilter(bounds) => 
        spaceFilters += bounds
      case TimeFilter(start, end) =>
        timeFilters += ( (start, end) )
    }

    if(spaceFilters.isEmpty) {
      val minKey = keyBounds.minKey.spatialKey
      val maxKey = keyBounds.maxKey.spatialKey
      spaceFilters += GridBounds(minKey.col, minKey.row, maxKey.col, maxKey.row)
    }

    if(timeFilters.isEmpty) {
      val minKey = keyBounds.minKey.temporalKey
      val maxKey = keyBounds.maxKey.temporalKey
      timeFilters += ( (minKey.time, maxKey.time) )

    }

    var rdds = mutable.ArrayBuffer[CassandraRDD[(String, ByteBuffer)]]()
    
    val ranges: Seq[CassandraRDD[(String, ByteBuffer)]] = (
      for {
        bounds <- spaceFilters
        (timeStart, timeEnd) <- timeFilters
      } yield {
        val p1 = SpaceTimeKey(bounds.colMin, bounds.rowMin, timeStart)
        val p2 = SpaceTimeKey(bounds.colMax, bounds.rowMax, timeEnd)
        
        val ranges = index.indexRanges(p1, p2)

        ranges
          .map { case (min: Long, max: Long) =>

            val start = f"${layerId.zoom}%02d_${min}%019d"
            val end   = f"${layerId.zoom}%02d_${max}%019d"
            rdds += rdd.where("id >= ? AND id <= ?", start, end)
            if (min == max)
              rdd.where("id = ?", start)
            else
              rdd.where("id >= ? AND id <= ?", start, end)
          }        
      }).flatten

    rdd.context.union(rdds.toSeq).asInstanceOf[RDD[(String, ByteBuffer)]] // Coalesce afterwards?
  }

  def reader(instance: CassandraInstance, metaData: CassandraLayerMetaData, keyBounds: KeyBounds[SpaceTimeKey], index: KeyIndex[SpaceTimeKey])(implicit sc: SparkContext): FilterableRasterRDDReader[SpaceTimeKey] =
    new FilterableRasterRDDReader[SpaceTimeKey] {
      def read(layerId: LayerId, filters: FilterSet[SpaceTimeKey]): RasterRDD[SpaceTimeKey] = {
        val CassandraLayerMetaData(rasterMetaData, _, _, tileTable) = metaData

        val rdd: CassandraRDD[(String, ByteBuffer)] = 
          sc.cassandraTable[(String, ByteBuffer)](instance.keyspace, tileTable).select("id", "value")

        val filteredRDD = applyFilter(rdd, layerId, filters, keyBounds, index)

        val tileRDD =
          filteredRDD.map { case (_, value) =>
                    val bytes = new Array[Byte](value.capacity)
                    value.get(bytes, 0, bytes.length)
                    val (key, tileBytes) = KryoSerializer.deserialize[(SpaceTimeKey, Array[Byte])](bytes)
                    val tile =
                      ArrayTile.fromBytes(
                        tileBytes,
                        rasterMetaData.cellType,
                        rasterMetaData.tileLayout.tileCols,
                        rasterMetaData.tileLayout.tileRows
                      )
                    (key, tile: Tile)
          }
    
        new RasterRDD(tileRDD, rasterMetaData)
      }
    }
}
