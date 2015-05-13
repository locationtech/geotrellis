package geotrellis.spark.io.s3.spatial

import geotrellis.spark._
import geotrellis.raster._
import geotrellis.spark.io.index._

import geotrellis.spark.io.s3._
import org.apache.spark.SparkContext
import com.typesafe.scalalogging.slf4j._
import scala.util.matching.Regex  

object SpatialRasterRDDReader extends RasterRDDReader[SpatialKey] with LazyLogging {
  val tileIdRx: Regex = """.+\/(\d+)""".r

  val indexToPath = encodeIndex
      
  val pathToIndex = (s: String) => {
    val tileIdRx(tileId) = s
    tileId.toLong
  }

  def setFilters(filterSet: FilterSet[SpatialKey], keyBounds: KeyBounds[SpatialKey], keyIndex: KeyIndex[SpatialKey]): Seq[(Long, Long)] = {    
    val ranges = 
      (for (filter <- filterSet.filters) yield {
        filter match {
          case SpaceFilter(b) =>
            keyIndex.indexRanges(SpatialKey(b.colMin, b.rowMin) -> SpatialKey(b.colMax, b.rowMax))
        }
      }).flatten

    if (ranges.isEmpty) // if we have no filter, expand to KeyBounds
      keyIndex.indexRanges(keyBounds)
    else
      ranges
  }
}
