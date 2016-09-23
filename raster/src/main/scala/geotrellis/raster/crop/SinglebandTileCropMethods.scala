package geotrellis.raster.crop

import geotrellis.vector._
import geotrellis.raster._
import geotrellis.raster.io.geotiff._

/**
  * A trait housing extension methods for cropping [[Tile]]s.
  */
trait SinglebandTileCropMethods extends TileCropMethods[Tile] {
  import Crop.Options

  /**
    * Given a [[GridBounds]] and some cropping options, produce a new
    * [[Tile]].
    */
  def crop(gb: GridBounds, options: Options): Tile = {
    val cropBounds =
      if(options.clamp)
        gb.intersection(self) match {
          case Some(intersection) => intersection
          case None =>
            throw new GeoAttrsError(s"Grid bounds do not intersect: $self crop $gb")
        }
      else
        gb

    val res =
      self match {
        case gtTile: GeoTiffTile =>
          gtTile.crop(gb)
        case _ =>
          CroppedTile(self, cropBounds)
      }

    if(options.force) res.toArrayTile else res
  }

  /**
    * Given a source Extent, a destination Extent, and some cropping
    * options, produce a cropped [[Raster]].
    */
  def crop(srcExtent: Extent, extent: Extent, options: Options): Tile =
    Raster(self, srcExtent).crop(extent, options).tile
}
