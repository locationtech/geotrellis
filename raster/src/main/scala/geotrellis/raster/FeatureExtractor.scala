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
import geotrellis.vector.{Feature, Geometry, Point, PointFeature}

import spire.syntax.cfor._

import scala.collection.mutable.ListBuffer

trait FeatureExtractor[I <: Geometry, T <: CellGrid[Int], O <: Geometry, D] {
  def features(geom: I, raster: Raster[T]): Array[Feature[O, D]]
}

object FeatureExtractor {
  def apply[I <: Geometry: FeatureExtractor[*, T, O, D], T <: CellGrid[Int], O <: Geometry, D] = implicitly[FeatureExtractor[I, T, O, D]]

  implicit def multibandTile[I <: Geometry] = new PointFeatureExtractor[I, MultibandTile, Array[Int]] {
    def features(geom: I, raster: Raster[MultibandTile]): Array[PointFeature[Array[Int]]] = {
      val buffer = ListBuffer[PointFeature[Array[Int]]]()

      Rasterizer.foreachCellByGeometry(geom, raster.rasterExtent) { case (col, row) =>
        val values = Array.ofDim[Int](raster.tile.bandCount)
        cfor(0)(_ < raster.tile.bandCount, _ + 1) { i => values(i) = raster.tile.band(i).get(col, row) }
        buffer += Feature(Point(raster.rasterExtent.gridToMap(col, row)), values)
      }

      buffer.toArray
    }
  }

  implicit def multibandTileDouble[I <: Geometry] = new PointFeatureExtractor[I, MultibandTile, Array[Double]] {
    def features(geom: I, raster: Raster[MultibandTile]): Array[PointFeature[Array[Double]]] = {
      val buffer = ListBuffer[PointFeature[Array[Double]]]()

      Rasterizer.foreachCellByGeometry(geom, raster.rasterExtent) { case (col, row) =>
        val values = Array.ofDim[Double](raster.tile.bandCount)
        cfor(0)(_ < raster.tile.bandCount, _ + 1) { i => values(i) = raster.tile.band(i).getDouble(col, row) }
        buffer += Feature(Point(raster.rasterExtent.gridToMap(col, row)), values)
      }

      buffer.toArray
    }
  }

  implicit def tile[I <: Geometry] = new PointFeatureExtractor[I, Tile, Int] {
    def features(geom: I, raster: Raster[Tile]): Array[PointFeature[Int]] =
      multibandTile[I].features(geom, raster.mapTile(MultibandTile(_))).map(_.mapData(_.head))
  }

  implicit def tileDouble[I <: Geometry] = new PointFeatureExtractor[I, Tile, Double] {
    def features(geom: I, raster: Raster[Tile]): Array[PointFeature[Double]] = {
      multibandTileDouble[I].features(geom, raster.mapTile(MultibandTile(_))).map(_.mapData(_.head))
    }
  }
}
