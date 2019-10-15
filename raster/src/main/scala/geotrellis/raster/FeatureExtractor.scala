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

trait FeatureExtractor[T <: CellGrid[Int], O <: Geometry, D] {
  def features(geom: Geometry, raster: Raster[T]): Iterator[Feature[O, D]]
}

object FeatureExtractor {
  def apply[T <: CellGrid[Int], O <: Geometry, D](implicit ev: FeatureExtractor[T, O, D]) = ev

  implicit def multibandTile = new PointFeatureExtractor[MultibandTile, Array[Int]] {
    def features(geom: Geometry, raster: Raster[MultibandTile]): Iterator[PointFeature[Array[Int]]] = {
      // val options: Rasterizer.Options = ???
      val mask = BitArrayTile.empty(cols = raster.cols, rows = raster.rows)
      Rasterizer.foreachCellByGeometry(geom, raster.rasterExtent) { case (col, row) =>
        mask.set(col, row, 1)
      }

      for {
        row <- Iterator.range(0, raster.rows)
        col <- Iterator.range(0, raster.cols)
          if mask.get(col, row) == 1
      } yield {
        val values = Array.ofDim[Int](raster.tile.bandCount)
        cfor(0)(_ < raster.tile.bandCount, _ + 1) { i => values(i) = raster.tile.band(i).get(col, row) }
        Feature(Point(raster.rasterExtent.gridToMap(col, row)), values)
      }
    }
  }

  implicit def multibandTileDouble = new PointFeatureExtractor[MultibandTile, Array[Double]] {
    def features(geom: Geometry, raster: Raster[MultibandTile]): Iterator[PointFeature[Array[Double]]] = {
      // val options: Rasterizer.Options = ???
      val mask = BitArrayTile.empty(cols = raster.cols, rows = raster.rows)
      Rasterizer.foreachCellByGeometry(geom, raster.rasterExtent) { case (col, row) =>
        mask.set(col, row, 1)
      }

      for {
        row <- Iterator.range(0, raster.rows)
        col <- Iterator.range(0, raster.cols)
          if mask.get(col, row) == 1
      } yield {
        val values = Array.ofDim[Double](raster.tile.bandCount)
        cfor(0)(_ < raster.tile.bandCount, _ + 1) { i => values(i) = raster.tile.band(i).getDouble(col, row) }
        Feature(Point(raster.rasterExtent.gridToMap(col, row)), values)
      }
    }
  }

  implicit val tileInt = new PointFeatureExtractor[Tile, Int] {
    def features(geom: Geometry, raster: Raster[Tile]): Iterator[PointFeature[Int]] =
      multibandTile.features(geom, raster.mapTile(MultibandTile(_))).map(_.mapData(_.head))
  }

  implicit val tileDouble = new PointFeatureExtractor[Tile, Double] {
    def features(geom: Geometry, raster: Raster[Tile]): Iterator[PointFeature[Double]] = {
      multibandTileDouble.features(geom, raster.mapTile(MultibandTile(_))).map(_.mapData(_.head))
    }
  }
}
