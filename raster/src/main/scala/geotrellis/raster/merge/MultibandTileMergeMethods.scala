package geotrellis.raster.merge

import geotrellis.raster.resample.ResampleMethod
import geotrellis.raster.{ArrayMultibandTile, Tile, MultibandTile}
import geotrellis.vector.Extent

/**
  * A trait containing extension methods related to MultibandTile
  * merging.
  */
trait MultibandTileMergeMethods extends TileMergeMethods[MultibandTile] {

  /**
    * Merge the respective bands of this MultibandTile and the other
    * one.
    */
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

  /**
    * Merge this [[MultibandTile]] with the other one.  All places in
    * the present tile that contain NODATA and are in the intersection
    * of the two given extents are filled-in with data from the other
    * tile.  A new MutlibandTile is returned.
    *
    * @param   extent        The extent of this MultiBandTile
    * @param   otherExtent   The extent of the other MultiBandTile
    * @param   other         The other MultiBandTile
    * @param   method        The resampling method
    * @return                A new MultiBandTile, the result of the merge
    */
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
