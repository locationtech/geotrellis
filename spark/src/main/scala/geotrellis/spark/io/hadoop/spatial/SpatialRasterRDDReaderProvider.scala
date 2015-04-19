package geotrellis.spark.io.hadoop.spatial

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

import scala.reflect._

// TODO: Refactor the writer and reader logic to abstract over the key type.
object SpatialRasterRDDReaderProvider extends RasterRDDReaderProvider[SpatialKey] with Logging {

  type KeyWritable = SpatialKeyWritable
  val kwClass = classOf[KeyWritable]
  type FMFInputFormat = SpatialFilterMapFileInputFormat
  val fmfClass = classOf[FMFInputFormat]
  val keyCTaggable = implicitly[ClassTag[SpatialKey]]

  def filterDefinition(
    filterSet: FilterSet[SpatialKey],
    keyBounds: KeyBounds[SpatialKey],
    keyIndex: KeyIndex[SpatialKey]
  ): FilterMapFileInputFormat.FilterDefinition[SpatialKey] = {
    val spaceFilters = mutable.ListBuffer[GridBounds]()

    filterSet.filters.foreach {
      case SpaceFilter(bounds) =>
        spaceFilters += bounds
    }

    if(spaceFilters.isEmpty) {
      val minKey = keyBounds.minKey
      val maxKey = keyBounds.maxKey
      spaceFilters += GridBounds(minKey.col, minKey.row, maxKey.col, maxKey.row)
    }

    val indexRanges = 
      (for {
        bounds <- spaceFilters
      } yield {
        val p1 = SpatialKey(bounds.colMin, bounds.rowMin)
        val p2 = SpatialKey(bounds.colMax, bounds.rowMax)

        val i1 = keyIndex.toIndex(p1)
        val i2 = keyIndex.toIndex(p2)

        keyIndex.indexRanges((p1, p2))
      }).flatten.toArray

    (filterSet, indexRanges)
  }
}
