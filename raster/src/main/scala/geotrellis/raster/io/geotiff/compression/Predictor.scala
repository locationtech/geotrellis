package geotrellis.raster.io.geotiff.compression

import geotrellis.raster.io.geotiff.tags._
import geotrellis.raster.io.geotiff.reader.MalformedGeoTiffException
import monocle.syntax._

object Predictor {
  val PREDICTOR_NONE = 1
  val PREDICTOR_HORIZONTAL = 2
  val PREDICTOR_FLOATINGPOINT = 3

  def apply(tags: Tags): Predictor = {
    (tags
      &|-> Tags._nonBasicTags
      ^|-> NonBasicTags._predictor get
    ) match {
      case None | Some(PREDICTOR_NONE) => 
        new Predictor { 
          val checkEndian = true
          def apply(bytes: Array[Byte], segmentIndex: Int) = bytes 
        }
      case Some(PREDICTOR_HORIZONTAL) =>
        HorizontalPredictor(tags)
      case Some(PREDICTOR_FLOATINGPOINT) =>
        FloatingPointPredictor(tags)
      case Some(i) =>
        throw new MalformedGeoTiffException(s"predictor tag $i is not valid (require 1, 2 or 3)")
    }
  }
}

trait Predictor {
  def checkEndian: Boolean

  def apply(bytes: Array[Byte], segmentIndex: Int): Array[Byte]
}


