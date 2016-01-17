package geotrellis.raster.merge

import geotrellis.raster.resample.ResampleMethod
import geotrellis.raster.{ArrayMultiBandTile, Tile, MultiBandTile}
import geotrellis.vector.Extent

trait MultiBandTileMergeMethods extends TileMergeMethods[MultiBandTile] {
  def merge(other: MultiBandTile): MultiBandTile = {
    val bands: Seq[Tile] =
      for {
        bandIndex <- 0 until self.bandCount
      } yield {
        val thisBand = self.band(bandIndex)
        val thatBand = other.band(bandIndex)
        thisBand.merge(thatBand)
      }

    ArrayMultiBandTile(bands)
  }

  def merge(extent: Extent, otherExtent: Extent, other: MultiBandTile, method: ResampleMethod): MultiBandTile = {
    val bands: Seq[Tile] =
      for {
        bandIndex <- 0 until self.bandCount
      } yield {
        val thisBand = self.band(bandIndex)
        val thatBand = other.band(bandIndex)
        thisBand.merge(extent, otherExtent, thatBand, method)
      }

    ArrayMultiBandTile(bands)
  }
}
