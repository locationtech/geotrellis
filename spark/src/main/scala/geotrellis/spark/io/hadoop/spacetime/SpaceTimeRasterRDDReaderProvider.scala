package geotrellis.spark.io.hadoop.spacetime

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.io.hadoop.formats._
import geotrellis.spark.io.index._
import geotrellis.spark.utils._
import geotrellis.raster._

import org.apache.spark.{SparkContext, Logging}
import org.apache.spark.rdd.RDD
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat

import scala.collection.mutable
import org.joda.time.{DateTimeZone, DateTime}

import scala.reflect._

// TODO: Refactor the writer and reader logic to abstract over the key type.
object SpaceTimeRasterRDDReaderProvider extends RasterRDDReaderProvider[SpaceTimeKey] {

  type KeyWritable = SpaceTimeKeyWritable
  val kwClass = classOf[KeyWritable]
  type FMFInputFormat = SpaceTimeFilterMapFileInputFormat
  val fmfClass = classOf[FMFInputFormat]
  val keyCTaggable = implicitly[ClassTag[SpaceTimeKey]]

  def filterDefinition(
    filterSet: FilterSet[SpaceTimeKey],
    keyBounds: KeyBounds[SpaceTimeKey],
    keyIndex: KeyIndex[SpaceTimeKey]
  ): FilterMapFileInputFormat.FilterDefinition[SpaceTimeKey] = {

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

    val indexRanges =
      (for {
        bounds <- spaceFilters
        (timeStart, timeEnd) <- timeFilters
      } yield {
        val p1 = SpaceTimeKey(bounds.colMin, bounds.rowMin, timeStart)
        val p2 = SpaceTimeKey(bounds.colMax, bounds.rowMax, timeEnd)

        val i1 = keyIndex.toIndex(p1)
        val i2 = keyIndex.toIndex(p2)

        keyIndex.indexRanges((p1, p2))
      }).flatten.toArray

    (filterSet, indexRanges)
  }
}
