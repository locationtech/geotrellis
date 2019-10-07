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

trait FeatureExtraction[I <: Geometry, T <: CellGrid[Int], O <: Geometry, D] {
  def features(geom: I, raster: Raster[T]): Array[Array[Feature[O, D]]]
}

object FeatureExtraction {
  def apply[I <: Geometry: FeatureExtraction[*, T, O, D], T <: CellGrid[Int], O <: Geometry, D] = implicitly[FeatureExtraction[I, T, O, D]]

  implicit def multibandTile[I <: Geometry] = new PointFeatureExtraction[I, MultibandTile, Int] {
    def features(geom: I, raster: Raster[MultibandTile]): Array[Array[PointFeature[Int]]] = {
      val arr = Array.ofDim[Array[PointFeature[Int]]](raster.tile.bandCount)

      cfor(0)(_ < raster.tile.bandCount, _ + 1) { i =>
        val buffer = ListBuffer[PointFeature[Int]]()
        Rasterizer.foreachCellByGeometry(geom, raster.rasterExtent) { case (col, row) =>
          buffer += Feature(Point(raster.rasterExtent.gridToMap(col, row)), raster.tile.band(i).get(col, row))
        }
        arr(i) = buffer.toArray
      }

      arr
    }
  }

  implicit def multibandTileDouble[I <: Geometry] = new PointFeatureExtraction[I, MultibandTile, Double] {
    def features(geom: I, raster: Raster[MultibandTile]): Array[Array[PointFeature[Double]]] = {
      val arr = Array.ofDim[Array[PointFeature[Double]]](raster.tile.bandCount)

      cfor(0)(_ < raster.tile.bandCount, _ + 1) { i =>
        val buffer = ListBuffer[PointFeature[Double]]()
        Rasterizer.foreachCellByGeometry(geom, raster.rasterExtent) { case (col, row) =>
          buffer += Feature(Point(raster.rasterExtent.gridToMap(col, row)), raster.tile.band(i).getDouble(col, row))
        }
        arr(i) = buffer.toArray
      }

      arr
    }
  }

  implicit def tile[I <: Geometry] = new PointFeatureExtraction[I, Tile, Int] {
    def features(geom: I, raster: Raster[Tile]): Array[Array[PointFeature[Int]]] =
      multibandTile[I].features(geom, raster.mapTile(MultibandTile(_)))
  }

  implicit def tileDouble[I <: Geometry] = new PointFeatureExtraction[I, Tile, Double] {
    def features(geom: I, raster: Raster[Tile]): Array[Array[PointFeature[Double]]] = {
      multibandTileDouble[I].features(geom, raster.mapTile(MultibandTile(_)))
    }
  }
}
