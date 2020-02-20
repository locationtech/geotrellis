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

import geotrellis.raster.rasterize.Rasterizer
import geotrellis.vector.{Feature, Geometry, Point, Polygon, Extent}
import geotrellis.util.MethodExtensions

import spire.syntax.cfor._


/** Type class to convert a raster into features of cell geometries */
trait CellFeatures[R, D] {

  /** Describe cell grid of the source raster */
  def cellGrid(raster: R): GridExtent[Long]

  /** Given a geometry, produce an iterator of cell features intersecting this geometry.
    * Relating geometry to raster cell grid is rasterization, options for this are given.
    * The call will decide how each cell will be represented as geometry (Point, Polygon)
    *
    * @note This method returns an Iterator because the feature representation of raster will require significantly
    *   more memory and it may not be possible to hold all features from a "large" raster tile in memory.
    *   It is assumed that either filter, fold or sink will be the next step.
    *
    * @note Conceptually rasterisation must happen but it is left to interface implementer to decide how to do it.
    *   Potentially the raster will need to be read in chunks.
    *
    * @param raster   source of cell values
    * @param geom     geometry filter, only cells under this geometry will be returned
    * @param options  rasterizer options that specify which cells should be considered as intersection geom
    * @param cellGeom function that converts a cell, specified by column and row into geometry (Point, Polygon)
    */
  def cellFeatures[G <: Geometry](raster: R, geom: Geometry, options: Rasterizer.Options, cellGeom: (Long, Long) => G): Iterator[Feature[G, D]]
}

object CellFeatures {
  def apply[R, D](implicit ev: CellFeatures[R, D]): CellFeatures[R, D] = ev

  /** Produce a CellFeatures instance given functions to retrieve a values and describe the cell grid.
    * This function is suitable for use with rasters that fit entirely in memory.
    */
  def make[R, D](getGrid: R => GridExtent[Int], cellValue: (R, Int, Int) => D): CellFeatures[R, D] =
    new CellFeatures[R, D] {
      def cellGrid(raster: R) = getGrid(raster).toGridType[Long]
      def cellFeatures[G <: Geometry](raster: R, geom: Geometry, options: Rasterizer.Options, cellGeom: (Long, Long) => G): Iterator[Feature[G, D]] = {
        val grid = getGrid(raster)
        val mask = BitArrayTile.empty(cols = grid.cols, rows = grid.rows)
        Rasterizer.foreachCellByGeometry(geom, grid.toRasterExtent) { case (col, row) => mask.set(col, row, 1) }
        for {
          row <- Iterator.range(0, grid.rows)
          col <- Iterator.range(0, grid.cols) if mask.get(col, row) == 1
        } yield {
          Feature(cellGeom(col, row), cellValue(raster, col, row))
        }
      }
    }

  implicit def multibandRasterIntInstance[T <: MultibandTile]: CellFeatures[Raster[T], Array[Int]] =
    make[Raster[T], Array[Int]](_.rasterExtent, { (raster, col, row) =>
        val values = Array.ofDim[Int](raster.tile.bandCount)
        cfor(0)(_ < raster.tile.bandCount, _ + 1) { i => values(i) = raster.tile.band(i).get(col, row) }
        values
    })

  implicit def multibandRasterDoubleInstance[T <: MultibandTile]: CellFeatures[Raster[T], Array[Double]] =
    make[Raster[T], Array[Double]](_.rasterExtent, { (raster, col, row) =>
        val values = Array.ofDim[Double](raster.tile.bandCount)
        cfor(0)(_ < raster.tile.bandCount, _ + 1) { i => values(i) = raster.tile.band(i).getDouble(col, row) }
        values
    })

  implicit def singlebandRasterIntInstance[T <: Tile]: CellFeatures[Raster[T], Int] =
    make[Raster[T], Int](_.rasterExtent, { (raster, col, row) => raster.tile.get(col, row) })

  implicit def singlebandRasterDoubleInstance[T <: Tile]: CellFeatures[Raster[T], Double] =
    make[Raster[T], Double](_.rasterExtent, { (raster, col, row) => raster.tile.getDouble(col, row) })



  trait Methods[R] extends MethodExtensions[R] {

    /** Produce cell features under geom as points of cell centers
      * @param geom geometry filter, only cells under this geometry will be returned
      */
    def cellFeaturesAsPoint[D](geom: Geometry)(implicit ev: CellFeatures[R, D]): Iterator[Feature[Point, D]] = {
      val grid = ev.cellGrid(self)
      val cellGeom: (Long, Long) => Point = { (col, row) =>
        val x = grid.gridColToMap(col)
        val y = grid.gridRowToMap(row)
        Point(x, y)
      }
      ev.cellFeatures(self, geom, Rasterizer.Options.DEFAULT, cellGeom)
    }

    /** Produce cell features under geom as polygons covering the cell
      * @param geom    geometry filter, only cells under this geometry will be returned
      * @param partial includes cells that partially intersect the geometry
      */
    def cellFeaturesAsArea[D](geom: Geometry, partial: Boolean = true)(implicit ev: CellFeatures[R, D]): Iterator[Feature[Polygon, D]] = {
      val grid = ev.cellGrid(self)
      val cellGeom: (Long, Long) => Polygon = { (col, row) =>
        val x = grid.gridColToMap(col)
        val y = grid.gridRowToMap(row)
        val hcw = grid.cellSize.width / 2.0
        val hch = grid.cellSize.height / 2.0
        Extent(x - hcw, y - hch, x + hcw, y + hch).toPolygon
      }
      ev.cellFeatures(self, geom, Rasterizer.Options(partial, PixelIsArea), cellGeom)
    }
  }
}
