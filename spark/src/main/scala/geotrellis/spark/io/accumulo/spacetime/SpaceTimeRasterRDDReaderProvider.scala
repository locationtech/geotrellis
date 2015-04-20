package geotrellis.spark.io.accumulo.spacetime

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.accumulo._
import geotrellis.spark.io.index._
import geotrellis.spark.utils._
import geotrellis.raster._
import geotrellis.index.zcurve._

import org.apache.hadoop.io.Text
import org.apache.hadoop.mapreduce.Job

import org.apache.accumulo.core.client.IteratorSetting
import org.apache.accumulo.core.client.mapreduce.InputFormatBase
import org.apache.accumulo.core.data.{Key, Value, Range => ARange}
import org.apache.accumulo.core.util.{Pair => APair}

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

import org.joda.time.{DateTimeZone, DateTime}

import scala.collection.mutable
import scala.collection.JavaConversions._
import scala.util.matching.Regex

object SpaceTimeRasterRDDReaderProvider extends RasterRDDReaderProvider[SpaceTimeKey] {

  def setFilters(
    job: Job,
    layerId: LayerId,
    filterSet: FilterSet[SpaceTimeKey],
    keyBounds: KeyBounds[SpaceTimeKey],
    keyIndex: KeyIndex[SpaceTimeKey]
  ): Unit = {
    InputFormatBase.setLogLevel(job, org.apache.log4j.Level.DEBUG)

    val ranges: Seq[ARange] = (
      FilterRanges.spatiotemporal(filterSet, keyBounds, keyIndex)
        .map { case (min: Long, max: Long) =>

          val start = f"${layerId.zoom}%02d_${min}%019d"
          val end   = f"${layerId.zoom}%02d_${max}%019d"
          if (min == max)
            ARange.exact(start)
          else
            new ARange(start, true, end, true)
        }
    )
    InputFormatBase.setRanges(job, ranges)

    var timeFilters = filterSet.filtersWithKey[SpaceTimeKey].map {
      case TimeFilter(start, end) => Some((start, end))
      case _ => None
    }.flatten
    if (timeFilters.isEmpty) {
      val minKey = keyBounds.minKey.temporalKey
      val maxKey = keyBounds.maxKey.temporalKey
      timeFilters = Seq((minKey.time, maxKey.time))
    }
    for ( (start, end) <- timeFilters) {
      val props =  Map(
        "startBound" -> start.toString,
        "endBound" -> end.toString,
        "startInclusive" -> "true",
        "endInclusive" -> "true"
      )
      InputFormatBase.addIterator(job,
        new IteratorSetting(2, "TimeColumnFilter", "org.apache.accumulo.core.iterators.user.ColumnSliceFilter", props))
    }

    InputFormatBase.fetchColumns(job, new APair(new Text(layerId.name), null: Text) :: Nil)
  }
}
