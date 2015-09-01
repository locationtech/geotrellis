package geotrellis.raster.mosaic

import geotrellis.raster.resample.ResampleMethod
import geotrellis.raster.{ArrayMultiBandTile, Tile, MultiBandTile}
import geotrellis.vector.Extent

trait MultiBandTileMergeMethods extends MergeMethods[MultiBandTile] {
  def merge(other: MultiBandTile): MultiBandTile = {
    val bands: Seq[Tile] =
      for {
        bandIndex <- 0 until tile.bandCount
      } yield {
        val thisBand = tile.band(bandIndex)
        val thatBand = other.band(bandIndex)
        thisBand.merge(thatBand)
      }

    ArrayMultiBandTile(bands)
  }

  def merge(extent: Extent, otherExtent: Extent, other: MultiBandTile, method: ResampleMethod): MultiBandTile = {
    val bands: Seq[Tile] =
      for {
        bandIndex <- 0 until tile.bandCount
      } yield {
        val thisBand = tile.band(bandIndex)
        val thatBand = other.band(bandIndex)
        thisBand.merge(extent, otherExtent, thatBand, method)
      }

    ArrayMultiBandTile(bands)
  }
}