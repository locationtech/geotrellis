package geotrellis.raster.merge

import geotrellis.raster.resample.ResampleMethod
import geotrellis.raster.{ArrayMultibandTile, Tile, MultibandTile}
import geotrellis.vector.Extent

trait MultibandTileMergeMethods extends TileMergeMethods[MultibandTile] {
  def merge(other: MultibandTile): MultibandTile = {
    val bands: Seq[Tile] =
      for {
        bandIndex <- 0 until self.bandCount
      } yield {
        val thisBand = self.band(bandIndex)
        val thatBand = other.band(bandIndex)
        thisBand.merge(thatBand)
      }

    ArrayMultibandTile(bands)
  }

  def merge(extent: Extent, otherExtent: Extent, other: MultibandTile, method: ResampleMethod): MultibandTile = {
    val bands: Seq[Tile] =
      for {
        bandIndex <- 0 until self.bandCount
      } yield {
        val thisBand = self.band(bandIndex)
        val thatBand = other.band(bandIndex)
        thisBand.merge(extent, otherExtent, thatBand, method)
      }

    ArrayMultibandTile(bands)
  }
}
