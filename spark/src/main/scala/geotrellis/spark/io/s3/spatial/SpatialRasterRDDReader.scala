package geotrellis.spark.io.s3.spatial

import geotrellis.spark._
import geotrellis.raster._
import geotrellis.spark.io.index._
import scala.reflect.ClassTag
import geotrellis.spark.io.s3._
import org.apache.spark.SparkContext
import com.typesafe.scalalogging.slf4j._
import scala.util.matching.Regex  

class SpatialRasterRDDReader[T: ClassTag] extends RasterRDDReader[SpatialKey, T] with LazyLogging {
  val tileIdRx: Regex = """.+\/(\d+)""".r

  val indexToPath = encodeIndex
      
  val pathToIndex = (s: String) => {
    val tileIdRx(tileId) = s
    tileId.toLong
  }
}
