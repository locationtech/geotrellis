package geotrellis.geotools

import org.geotools.coverage.grid.GridCoverage2D

trait ToGridCoverage2DMethods {
  def toGridCoverage2D(): GridCoverage2D
}
