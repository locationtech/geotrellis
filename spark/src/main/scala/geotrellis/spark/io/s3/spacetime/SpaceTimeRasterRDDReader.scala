package geotrellis.spark.io.s3.spacetime

import geotrellis.spark._
import geotrellis.raster._
import geotrellis.spark.io.index._
import geotrellis.spark.io.s3._

import org.apache.spark.SparkContext
import com.typesafe.scalalogging.slf4j._
import com.github.nscala_time.time.Imports._
import scala.util.matching.Regex
import scala.reflect.ClassTag
import scala.collection.mutable

class SpaceTimeRasterRDDReader[T: ClassTag] extends RasterRDDReader[SpaceTimeKey, T] with LazyLogging {
  val tileIdRx: Regex = """.+\/(\d+)-\d{4}.+$""".r    

  val indexToPath = encodeIndex
      
  val pathToIndex = (s: String) => {
    val tileIdRx(tileId) = s
    tileId.toLong
  }
}