package geotrellis

import geotrellis.raster._
import geotrellis.util.MethodExtensions

import org.geotools.coverage.grid.GridCoverage2D

package object geotools {
  implicit class withSinglebandRasterToGridCoverage2DMethods(val self: Raster[Tile]) extends MethodExtensions[Raster[Tile]]
      with SinglebandRasterToGridCoverage2DMethods

  implicit class withSinglebandProjectedRasterToGridCoverage2DMethods[T <: Tile](val self: ProjectedRaster[T]) extends MethodExtensions[ProjectedRaster[T]]
      with SinglebandProjectedRasterToGridCoverage2DMethods[T]

  implicit class withMultibandRasterToGridCoverage2DMethods(val self: Raster[MultibandTile]) extends MethodExtensions[Raster[MultibandTile]]
      with MultibandRasterToGridCoverage2DMethods

  implicit class withMultibandProjectedRasterToGridCoverage2DMethods[T <: MultibandTile](val self: ProjectedRaster[T]) extends MethodExtensions[ProjectedRaster[T]]
      with MultibandProjectedRasterToGridCoverage2DMethods[T]


  implicit class withGridCoverage2DConversionMethods(val self: GridCoverage2D) extends MethodExtensions[GridCoverage2D]
      with GridCoverage2DConversionMethods
}
