package geotrellis.data

import geotrellis._
import geotrellis.data.geotiff._

/**
 * This GeoTiffWriter is deprecated.
 *
 * See geotrellis.data.geotiff.Encoder for the preferred approach to
 * encoding rasters to geotiff files.
 */
object GeoTiffWriter extends Writer {
  def rasterType = "geotiff" 
  def dataType = ""

  def write(path:String, raster:Raster, name:String) {
    val settings = raster.data.getType match {
      case TypeBit | TypeByte => Settings.int8
      case TypeShort => Settings.int16
      case TypeInt => Settings.int32
      case TypeFloat => Settings.float32
      case TypeDouble => Settings.float64
    }
    Encoder.writePath(path, raster, settings)
  }
}
