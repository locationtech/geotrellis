package geotrellis.spark.io.accumulo.spatial

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.accumulo._
import geotrellis.spark.io.index._
import geotrellis.spark.utils._
import geotrellis.raster._

import org.apache.hadoop.io.Text
import org.apache.hadoop.mapreduce.Job

import org.apache.accumulo.core.client.mapreduce.InputFormatBase
import org.apache.accumulo.core.data.{Key, Value, Range => ARange}
import org.apache.accumulo.core.util.{Pair => APair}

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

import scala.collection.JavaConversions._

object SpatialRasterRDDReaderProvider extends RasterRDDReaderProvider[SpatialKey] {

  def setFilters(job: Job, layerId: LayerId, filterSet: FilterSet[SpatialKey], keyBounds: KeyBounds[SpatialKey], keyIndex: KeyIndex[SpatialKey]): Unit = {
    var tileBoundSet = false

    val ranges =
      FilterRanges.spatial(filterSet, keyBounds, keyIndex).map {
        case (min, max) =>
          new ARange(rowId(layerId, min), rowId(layerId, max))
      }
    InputFormatBase.setRanges(job, ranges)

    //Set the filter for layer we need
    InputFormatBase.fetchColumns(job, new APair(new Text(layerId.name), null: Text) :: Nil)
  }
}
