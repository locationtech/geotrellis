package geotrellis.process

import scala.io.Source
import java.io.File
import geotrellis._
import geotrellis.util._

abstract class RasterLayerType()

case object ArgFile extends RasterLayerType
case object AsciiFile extends RasterLayerType
case object Tiled extends RasterLayerType
case object ConstantRaster extends RasterLayerType

object RasterLayerType {
  implicit def stringToRasterLayerType(s:String) = {
    s match {
      case "constant" => ConstantRaster
      case "asc" => AsciiFile
      case "arg" => ArgFile
      case "tiled" => Tiled
      case _ => sys.error(s"$s is not a valid raster layer type.")
    }
  }
}
