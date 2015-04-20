package geotrellis.spark.io.s3.spacetime

import geotrellis.spark._
import geotrellis.raster._
import geotrellis.spark.io._
import geotrellis.spark.io.index._
import geotrellis.spark.io.s3._

import org.apache.spark.SparkContext
import com.typesafe.scalalogging.slf4j._
import com.github.nscala_time.time.Imports._

import scala.collection.mutable

object SpaceTimeRasterRDDReaderProvider extends RasterRDDReaderProvider[SpaceTimeKey] with LazyLogging {
  def setFilters(filterSet: FilterSet[SpaceTimeKey], keyBounds: KeyBounds[SpaceTimeKey], keyIndex: KeyIndex[SpaceTimeKey]): Seq[(Long, Long)] = {
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
    
    (for {
      bounds <- spaceFilters
      (timeStart, timeEnd) <- timeFilters
    } yield {
      keyIndex.indexRanges(
        SpaceTimeKey(bounds.colMin, bounds.rowMin, timeStart), 
        SpaceTimeKey(bounds.colMax, bounds.rowMax, timeEnd))
    }).flatten
  }
}
