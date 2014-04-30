package geotrellis.gdal

import org.gdal.{scala => GDAL}
import org.gdal.scala.Gdal

import geotrellis._
import geotrellis.raster._

object GdalReader {
  def read(path: String, band: Int = 1): Raster = {
    val gdalRaster: GDAL.Raster = Gdal.open(path)

    val extent = Extent(gdalRaster.xmin, 
                        gdalRaster.ymin,
                        gdalRaster.xmax,
                        gdalRaster.ymax)
    val (lcols, lrows) = (gdalRaster.cols, gdalRaster.rows)

    if(lcols * lrows > Int.MaxValue) 
      sys.error(s"Cannot read this raster, cols * rows exceeds maximum array index ($lcols * $lrows)")

    val (cols, rows) = (lcols.toInt, lrows.toInt)

    val rasterExtent = RasterExtent(extent, cols, rows)

    val rasterBand = gdalRaster.bands(band - 1)
    val rasterType = rasterBand.rasterType match {
      case GDAL.TypeUnknown => geotrellis.TypeDouble
      case GDAL.TypeByte => geotrellis.TypeShort // accounts for unsigned
      case GDAL.TypeUInt16 => geotrellis.TypeInt // accounts for unsigned
      case GDAL.TypeInt16 => geotrellis.TypeShort
      case GDAL.TypeUInt32 => geotrellis.TypeFloat // accounts for unsigned
      case GDAL.TypeInt32 => geotrellis.TypeInt
      case GDAL.TypeFloat32 => geotrellis.TypeFloat
      case GDAL.TypeFloat64 => geotrellis.TypeDouble
      case GDAL.TypeCInt16 => ???
      case GDAL.TypeCInt32 => ???
      case GDAL.TypeCFloat32 => ???
      case GDAL.TypeCFloat64 => ???
    }

    val data = 
      (rasterType match {
        case TypeShort =>
          ShortArrayRasterData(rasterBand.dataShort, cols, rows)
        case TypeInt => 
          IntArrayRasterData(rasterBand.dataInt, cols, rows)
        case TypeFloat =>
          FloatArrayRasterData(rasterBand.dataFloat, cols, rows)
        case TypeDouble =>
          DoubleArrayRasterData(rasterBand.dataDouble, cols, rows)
      }).mutable

    // Replace NODATA values
    rasterBand.noDataValue match {
      case Some(nd) =>
        var col = 0
        while(col < cols) {
          var row = 0
          while(row < rows) {
            if(data.getDouble(col,row) == nd) { data.set(col, row, NODATA) }
            row += 1
          }
          col += 1
        }
      case None =>
    }

    Raster(data, rasterExtent)
  }
}
