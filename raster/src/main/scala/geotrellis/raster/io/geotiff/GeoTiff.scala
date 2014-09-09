package geotrellis.raster.io.geotiff

import geotrellis.proj4.CRS
import geotrellis.raster._
import geotrellis.vector.Extent

object GeoTiff {

  def render(tile: Tile, extent: Extent, crs: CRS, compression: Compression): Array[Byte] = {
    val settings = tile.cellType match {
      case TypeBit | TypeByte => Settings(ByteSample, Signed, compression)
      case TypeShort => Settings(ShortSample, Signed, compression)
      case TypeInt => Settings(IntSample, Signed, compression)
      case TypeFloat => Settings(IntSample, Floating, compression)
      case TypeDouble => Settings(LongSample, Floating, compression)
    }

    val rasterExtent = RasterExtent(extent, tile.cols, tile.rows)

    Encoder.writeBytes(tile, rasterExtent, crs, settings)
  }

}
