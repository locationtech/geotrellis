package geotrellis.gdal

import geotrellis.raster._
import geotrellis.vector.Extent

object GdalReader {
  def read(path: String, band: Int = 1): (Tile, RasterExtent) = {
    val gdalRaster: RasterDataSet = Gdal.open(path)

    try {
      val extent = Extent(
        gdalRaster.xmin,
        gdalRaster.ymin,
        gdalRaster.xmax,
        gdalRaster.ymax
      )
      val (lcols, lrows) = (gdalRaster.cols, gdalRaster.rows)

      if(lcols * lrows > Int.MaxValue)
        sys.error(s"Cannot read this raster, cols * rows exceeds maximum array index ($lcols * $lrows)")

      val (cols, rows) = (lcols.toInt, lrows.toInt)

      val rasterExtent = RasterExtent(extent, cols, rows)

      (gdalRaster.bands(band - 1).toTile, rasterExtent)
    } finally {
      gdalRaster.close
    }
  }
}
