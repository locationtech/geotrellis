/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
