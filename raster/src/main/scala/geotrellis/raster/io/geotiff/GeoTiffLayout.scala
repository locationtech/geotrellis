package geotrellis.raster.io.geotiff

import geotrellis.raster._

abstract sealed class GeoTiffLayout

case class Tiled(blockCols: Int = 256, blockRows: Int = 256) extends GeoTiffLayout

// Trait used only for implicit conversion of object
private[geotiff] trait TiledGeoTiffLayout

object Tiled extends TiledGeoTiffLayout {
  implicit def objectToLayout(t: TiledGeoTiffLayout): Tiled = Tiled()
}

class Striped(rowsPerStrip: Option[Int]) extends GeoTiffLayout {
  def rowsPerStrip(rows: Int, bandType: BandType): Int =
    rowsPerStrip match {
      case Some(ris) => ris
      case None =>
        // strip height defaults to a value such that one strip is 8K or less.
        val rowSize = rows * bandType.bytesPerSample
        val ris = 8000 / rowSize
        if(ris == 0) 1
        else ris
    }
}

// Trait used only for implicit conversion of object
private[geotiff] trait StripedGeoTiffLayout

object Striped extends StripedGeoTiffLayout {
  def apply(rowsPerStrip: Int): Striped = new Striped(Some(rowsPerStrip))
  def apply(): Striped = new Striped(None)

  implicit def objectToLayout(s: StripedGeoTiffLayout): Striped = Striped()
}
