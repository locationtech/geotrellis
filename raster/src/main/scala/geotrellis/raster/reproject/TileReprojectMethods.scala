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

package geotrellis.raster.reproject

import geotrellis.raster._
import geotrellis.proj4._
import geotrellis.raster.resample.{ResampleMethod, ResampleGrid}
import geotrellis.vector.Extent
import geotrellis.util.MethodExtensions

import spire.math.Integral

//gt
  //val gridExtent: GridExtent[Long],
  //val resampleMethod: ResampleMethod = NearestNeighbor,
  //val targetCellType: Option[TargetCellType] = None

//tiff
  //resampleGrid: ResampleGrid[Long],
  //method: ResampleMethod = NearestNeighbor,
  //strategy: OverviewStrategy = AutoHigherResolution,
  //private[vlm] val targetCellType: Option[TargetCellType] = None

trait TileReprojectMethods[T <: CellGrid[Int]] extends MethodExtensions[T] {
  def reproject[N: Integral](srcExtent: Extent, transform: Transform, inverseTransform: Transform, resampleGrid: ResampleGrid[N]): Raster[T]

  def reproject[N: Integral](srcExtent: Extent, src: CRS, dest: CRS, resampleGrid: ResampleGrid[N]): Raster[T]
}
