package geotrellis.spark.io.cassandra.spacetime

import java.nio.ByteBuffer

import org.joda.time.DateTime

import scala.collection.mutable
import scala.util.matching.Regex

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.io.cassandra._
import geotrellis.spark.io.json._
import geotrellis.spark.io.index._
import geotrellis.spark.io.index.zcurve._
import geotrellis.spark.utils._

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

import com.datastax.spark.connector.rdd.CassandraRDD
import com.datastax.spark.connector._

object SpaceTimeRasterRDDReader extends RasterRDDReader[SpaceTimeKey] {
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

  def applyFilter(
    rdd: CassandraRDD[(String, ByteBuffer)],
    layerId: LayerId,
    queryKeyBounds: Seq[KeyBounds[SpaceTimeKey]],
    keyBounds: KeyBounds[SpaceTimeKey],
    index: KeyIndex[SpaceTimeKey]
  ): RDD[(String, ByteBuffer)] = {

    val spaceFilters = mutable.ListBuffer[GridBounds]()
    val timeFilters = mutable.ListBuffer[(DateTime, DateTime)]()

    val ranges = queryKeyBounds.map{ index.indexRanges(_) }.flatten
    logInfo(s"queryKeyBounds has ${ranges.length} ranges")

    for (range <- ranges) {
      logInfo(s"range has ${range.toString()} ")
    }

    // TODO need to get this extra filtering back? queryKeyBounds approach?
    // filterSet.filters.foreach {
    //  case SpaceFilter(bounds) =>
    //    spaceFilters += bounds
    //  case TimeFilter(start, end) =>
    //    timeFilters += ( (start, end) )
    // }
    for ( qbounds <- queryKeyBounds ) {
      /*
      KeyBounds(minKey, maxKey of K Type)

      SpaceTimeKey(col: Int, row: Int, time: DateTime)
      spatialKey = SpatialKey(col, row)
      temporalKey = TemporalKey(time)
       */

      val spaceMinKey = qbounds.minKey.spatialKey
      val spaceMaxKey = qbounds.maxKey.spatialKey
      spaceFilters += GridBounds(spaceMinKey.col, spaceMinKey.row, spaceMaxKey.col, spaceMaxKey.row)
      logInfo(s"qBounds GridBounds(${spaceMinKey.col}, ${spaceMinKey.row}, ${spaceMaxKey.col}, ${spaceMaxKey.row})")

      val timeMinKey = qbounds.minKey.temporalKey
      val timeMaxKey = qbounds.maxKey.temporalKey
      timeFilters += ( (timeMinKey.time, timeMaxKey.time) )
      logInfo(s"qBounds ( (${timeMinKey.time.toString}, ${timeMaxKey.time.toString}) )")
    }

    if(spaceFilters.isEmpty) {
      val minKey = keyBounds.minKey.spatialKey
      val maxKey = keyBounds.maxKey.spatialKey
      spaceFilters += GridBounds(minKey.col, minKey.row, maxKey.col, maxKey.row)
      logInfo(s"keyBounds GridBounds(${minKey.col}, ${minKey.row}, ${maxKey.col}, ${maxKey.row})")
    }

    if(timeFilters.isEmpty) {
      val minKey = keyBounds.minKey.temporalKey
      val maxKey = keyBounds.maxKey.temporalKey
      timeFilters += ( (minKey.time, maxKey.time) )
      logInfo(s"keyBounds ( (${minKey.time.toString}, ${maxKey.time.toString}) )")
    }

    val rdds = mutable.ArrayBuffer[CassandraRDD[(String, ByteBuffer)]]()

    for {
      bounds <- spaceFilters
      (timeStart, timeEnd) <- timeFilters
    } yield {
      val p1 = SpaceTimeKey(bounds.colMin, bounds.rowMin, timeStart)
      val p2 = SpaceTimeKey(bounds.colMax, bounds.rowMax, timeEnd)

      val ranges = index.indexRanges(p1, p2)

      logInfo(s"for yield has ${ranges.length} ranges")

      for (range <- ranges) {
        logInfo(s"for yield range has ${range.toString()} ")
      }

      ranges
        .foreach { case (min: Long, max: Long) =>
          if (min == max)
            rdds += rdd.where("zoom = ? AND indexer = ?", layerId.zoom, min)
          else
            rdds += rdd.where("zoom = ? AND indexer >= ? AND indexer <= ?", layerId.zoom, min.toString, max.toString)
        }
    }

    rdd.context.union(rdds.toSeq).asInstanceOf[RDD[(String, ByteBuffer)]]
  }
}
