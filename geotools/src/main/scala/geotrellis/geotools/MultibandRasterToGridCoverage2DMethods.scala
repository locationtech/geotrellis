package geotrellis.geotools

import geotrellis.raster._
import geotrellis.util.MethodExtensions

import org.geotools.coverage.grid._

trait MultibandRasterToGridCoverage2DMethods extends MethodExtensions[Raster[MultibandTile]] {
  def toGridCoverage2D(): GridCoverage2D = {
    ???
  }
}
