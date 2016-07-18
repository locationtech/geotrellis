package geotrellis.geotools

import geotrellis.raster._
import geotrellis.util.MethodExtensions

import org.geotools.coverage.grid._

trait MultibandRasterToGridCoverage2DMethods extends MethodExtensions[Raster[MultibandTile]] with ToGridCoverage2DMethods {
  def toGridCoverage2D(): GridCoverage2D =
    GridCoverage2DConverters.convertToGridCoverage2D(self)
}

trait MultibandProjectedRasterToGridCoverage2DMethods[T <: MultibandTile] extends MethodExtensions[ProjectedRaster[T]] with ToGridCoverage2DMethods {
  def toGridCoverage2D(): GridCoverage2D =
    GridCoverage2DConverters.convertToGridCoverage2D(self.raster, self.crs)
}
