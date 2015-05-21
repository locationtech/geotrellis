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
}
