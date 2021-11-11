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

package geotrellis.raster

import geotrellis.vector._


/**
  * The companion object for the [[Raster]] type.
  */
object Raster {
  def apply[T <: CellGrid[Int]](feature: PolygonFeature[T]): Raster[T] =
    Raster(feature.data, feature.geom.extent)

  /**
    * Implicit conversion from a [[Tile]], Extent pair to a
    * [[Raster]].
    */
  @deprecated("Implicit conversions considered unsafe", "2.1.1")
  implicit def tupToRaster(tup: (Tile, Extent)): Raster[Tile] =
    Raster(tup._1, tup._2)

  /**
    * Implicit conversion from an Extent, [[Tile]] pair to a
    * [[Raster]].
    */
  @deprecated("Implicit conversions considered unsafe", "2.1.1")
  implicit def tupSwapToRaster(tup: (Extent, Tile)): Raster[Tile] =
    Raster(tup._2, tup._1)

  /**
    * Implicit conversion from a [[Raster]] to a PolygonFeature.
    */
  @deprecated("Implicit conversions considered unsafe", "2.1.1")
  implicit def rasterToFeature[T <: CellGrid[Int]](r: Raster[T]): PolygonFeature[T] =
    r.asFeature()

  /**
    * Implicit conversion from a PolygonFeature to a [[Raster]].
    */
  @deprecated("Implicit conversions considered unsafe", "2.1.1")
  implicit def featureToRaster[T <: CellGrid[Int]](feature: PolygonFeature[T]): Raster[T] =
     apply(feature)
}

/**
  * The [[Raster]] type.
  */
case class Raster[+T <: CellGrid[Int]](tile: T, extent: Extent) extends CellGrid[Int] {

  /**
    * Return the [[RasterExtent]] that is correspondent to this
    * [[Raster]].
    */
  def rasterExtent: RasterExtent = RasterExtent(extent, tile.cols, tile.rows)

  /**
    * Return the [[CellSize]] of this [[Raster]].
    */
  def cellSize: CellSize = rasterExtent.cellSize

  def cols: Int = tile.cols
  def rows: Int = tile.rows
  def cellType: CellType = tile.cellType

  /**
    * Return the PolygonFeature associated with the extent of this
    * [[Raster]].
    */
  def asFeature(): PolygonFeature[T] = PolygonFeature(extent.toPolygon(), tile: T)

  def mapTile[A <: CellGrid[Int]](f: T => A): Raster[A] = Raster(f(tile), extent)

  def _1: T = tile

  def _2: Extent = extent
}
