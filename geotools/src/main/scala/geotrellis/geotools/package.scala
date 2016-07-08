package geotrellis

import geotrellis.raster._
import geotrellis.util.MethodExtensions
import geotrellis.vector._

import org.geotools.coverage.grid.GridCoverage2D
import org.opengis.feature.simple.SimpleFeature


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

  implicit class withGeometryToSimpleFeatureMethods[G <: Geometry](val self: G) extends MethodExtensions[G]
      with GeometryToSimpleFeatureMethods[G]

  implicit class withSimpleFeatureMethods(val self: SimpleFeature) extends MethodExtensions[SimpleFeature]
      with SimpleFeatureToFeatureMethods
      with SimpleFeatureToGeometryMethods

  implicit class withFeatureToSimpleFeatureMethods[G <: Geometry, T](val self: Feature[G, T])
      extends MethodExtensions[Feature[G, T]]
      with FeatureToSimpleFeatureMethods[G, T]
}
