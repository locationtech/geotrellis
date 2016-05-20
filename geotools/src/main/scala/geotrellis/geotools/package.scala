package geotrellis

import geotrellis.raster._
import geotrellis.util.MethodExtensions

package object geotools {
  implicit class withSinglebandRasterToGridCoverage2DMethods(val self: Raster[Tile]) extends MethodExtensions[Raster[Tile]]
      with SinglebandRasterToGridCoverage2DMethods

  implicit class withMultibandRasterToGridCoverage2DMethods(val self: Raster[MultibandTile]) extends MethodExtensions[Raster[MultibandTile]]
      with MultibandRasterToGridCoverage2DMethods
}
