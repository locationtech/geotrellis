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

    val rdds = mutable.ArrayBuffer[CassandraRDD[(String, ByteBuffer)]]()
    
    for {
      bounds <- spaceFilters
      (timeStart, timeEnd) <- timeFilters
    } yield {
      val p1 = SpaceTimeKey(bounds.colMin, bounds.rowMin, timeStart)
      val p2 = SpaceTimeKey(bounds.colMax, bounds.rowMax, timeEnd)
      
      val ranges = index.indexRanges(p1, p2)
      
      ranges
        .foreach { case (min: Long, max: Long) =>
          if (min == max)
            rdds += rdd.where("zoom = ? AND indexer = ?", layerId.zoom, min)
          else
            rdds += rdd.where("zoom = ? AND indexer >= ? AND indexer <= ?", layerId.zoom, min.toString, max.toString)
        }       
    }

    rdd.context.union(rdds.toSeq).asInstanceOf[RDD[(String, ByteBuffer)]] // Coalesce afterwards?
  }

  def reader(metaData: CassandraLayerMetaData, keyBounds: KeyBounds[SpaceTimeKey], index: KeyIndex[SpaceTimeKey])(implicit session: CassandraSession, sc: SparkContext): FilterableRasterRDDReader[SpaceTimeKey] =
    new FilterableRasterRDDReader[SpaceTimeKey] {
      def read(layerId: LayerId, filters: FilterSet[SpaceTimeKey]): RasterRDD[SpaceTimeKey] = {
        val CassandraLayerMetaData(rasterMetaData, _, _, tileTable) = metaData

        val rdd: CassandraRDD[(String, ByteBuffer)] = 
          sc.cassandraTable[(String, ByteBuffer)](session.keySpace, tileTable).select("reverse_index", "value")

        
        val filteredRDD = {
          if (filters.isEmpty) {
            rdd.where("zoom = ?", layerId.zoom)
          } else {
            applyFilter(rdd, layerId, filters, keyBounds, index)
          }
        }

        val tileRDD =
          filteredRDD.map { case (_, value) =>
                    val (key, tileBytes) = KryoSerializer.deserialize[(SpaceTimeKey, Array[Byte])](value)
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
