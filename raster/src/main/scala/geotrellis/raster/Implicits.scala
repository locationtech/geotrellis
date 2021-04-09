/*
 * Copyright 2019 Azavea
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

package geotrellis.raster

import geotrellis.vector.Point
import geotrellis.vector.withExtraPointMethods
import geotrellis.util.{MethodExtensions, np}

object Implicits extends Implicits

trait Implicits
    extends geotrellis.raster.costdistance.Implicits
    with geotrellis.raster.crop.Implicits
    with geotrellis.raster.density.Implicits
    with geotrellis.raster.distance.Implicits
    with geotrellis.raster.equalization.Implicits
    with geotrellis.raster.hydrology.Implicits
    with geotrellis.raster.interpolation.Implicits
    with geotrellis.raster.io.json.Implicits
    with geotrellis.raster.mapalgebra.focal.Implicits
    with geotrellis.raster.mapalgebra.focal.hillshade.Implicits
    with geotrellis.raster.mapalgebra.local.Implicits
    with geotrellis.raster.mapalgebra.zonal.Implicits
    with geotrellis.raster.mask.Implicits
    with geotrellis.raster.matching.Implicits
    with geotrellis.raster.merge.Implicits
    with geotrellis.raster.prototype.Implicits
    with geotrellis.raster.rasterize.Implicits
    with geotrellis.raster.regiongroup.Implicits
    with geotrellis.raster.render.Implicits
    with geotrellis.raster.reproject.Implicits
    with geotrellis.raster.resample.Implicits
    with geotrellis.raster.sigmoidal.Implicits
    with geotrellis.raster.split.Implicits
    with geotrellis.raster.summary.Implicits
    with geotrellis.raster.summary.polygonal.Implicits
    with geotrellis.raster.transform.Implicits
    with geotrellis.raster.vectorize.Implicits
    with geotrellis.raster.viewshed.Implicits {

  // Implicit method extension for core types

  implicit class withTileMethods(val self: Tile) extends MethodExtensions[Tile]
      with DelayedConversionTileMethods

  implicit class withMultibandTileMethods(val self: MultibandTile) extends MethodExtensions[MultibandTile]
      with DelayedConversionMultibandTileMethods

  implicit class SinglebandRasterAnyRefMethods(val self: SinglebandRaster) extends AnyRef {
    def getValueAtPoint(point: Point): Int =
      getValueAtPoint(point.x, point.y)

    def getValueAtPoint(x: Double, y: Double): Int =
      self.tile.get(
        self.rasterExtent.mapXToGrid(x),
        self.rasterExtent.mapYToGrid(y)
      )

    def getDoubleValueAtPoint(point: Point): Double =
      getDoubleValueAtPoint(point.x, point.y)

    def getDoubleValueAtPoint(x: Double, y: Double): Double =
      self.tile.getDouble(
        self.rasterExtent.mapXToGrid(x),
        self.rasterExtent.mapYToGrid(y)
      )
  }

  implicit class TraversableTileExtensions(rs: Traversable[Tile]) {
    def assertEqualDimensions(): Unit =
      if(Set(rs.map(_.dimensions)).size != 1) {
        val dimensions = rs.map(_.dimensions).toSeq
        throw GeoAttrsError("Cannot combine tiles with different dimensions." +
          s"$dimensions are not all equal")
      }
  }

  implicit class TileTupleExtensions(t: (Tile, Tile)) {
    def assertEqualDimensions(): Unit =
      if(t._1.dimensions != t._2.dimensions) {
        throw GeoAttrsError("Cannot combine rasters with different dimensions." +
          s"${t._1.dimensions} does not match ${t._2.dimensions}")
      }
  }

  implicit class TilePercentileExtensions(tile: Tile) {
    /**
      * Compute percentile at the given breaks using the same algorithm as numpy
      *
      * https://docs.scipy.org/doc/numpy/reference/generated/numpy.percentile.html
      * https://en.wikipedia.org/wiki/Percentile
      *
      * @param pctBreaks
      * @return
      */
    def percentile(pctBreaks: Array[Double]): Array[Double] = {
      np.percentile(tile.toArrayDouble().filter(isData(_)), pctBreaks)
    }

    /**
      * Compute percentile at the given break using the same algorithm as numpy
      *
      * https://docs.scipy.org/doc/numpy/reference/generated/numpy.percentile.html
      * https://en.wikipedia.org/wiki/Percentile
      *
      * @param pctBreak
      * @return
      */
    def percentile(pctBreak: Double): Double = {
      np.percentile(tile.toArrayDouble().filter(isData(_)), pctBreak)
    }
  }

  implicit class withCellFeaturesMethods[R](val self: R) extends CellFeatures.Methods[R]
}
