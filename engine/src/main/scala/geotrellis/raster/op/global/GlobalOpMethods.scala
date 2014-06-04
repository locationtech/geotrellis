/*
 * Copyright (c) 2014 Azavea.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.raster.op.global

import geotrellis._
import geotrellis.feature._
import geotrellis.raster._
import geotrellis.source._

trait GlobalOpMethods[+Repr <: RasterSource] { self: Repr =>
  def convolve(kernel: Kernel) =
    self.global(Convolve(_, kernel))

  def costDistance(points: Seq[(Int, Int)]) = 
    self.global(CostDistance(_, points))

  def rescale(newMin: Int, newMax: Int) =
    self.global(_.rescale(newMin, newMax))

  def rescale(newMin: Double, newMax: Double) =
    self.global(_.rescale(newMin, newMax))

  def toVector() = 
    self.converge.mapOp { tile =>
      rasterDefinition.map { rd => ToVector(tile, rd.rasterExtent.extent) }
    }

  def regionGroup(options: RegionGroupOptions = RegionGroupOptions.default) =
    self.converge.map(RegionGroup(_, options))

  def verticalFlip() =
    self.global(VerticalFlip(_))

  def viewshed(p: Point, exact: Boolean = false) =
    if(exact)
      self.globalOp { r => 
        rasterDefinition.map { rd =>
          val (col, row) = rd.rasterExtent.mapToGrid(p.x, p.y)
          Viewshed(r, col, row) 
        }
      }
    else
      self.globalOp { r => 
        rasterDefinition.map { rd =>
          val (col, row) = rd.rasterExtent.mapToGrid(p.x, p.y)
          ApproxViewshed(r, col, row)
        }
      }

  def viewshedOffsets(p: Point, exact: Boolean = false) =
    if(exact)
      self.globalOp { r => 
        rasterDefinition.map { rd =>
          val (col, row) = rd.rasterExtent.mapToGrid(p.x, p.y)
          Viewshed.offsets(r, col, row) 
        }
      }
    else
      self.globalOp { r => 
        rasterDefinition.map { rd =>
          val (col, row) = rd.rasterExtent.mapToGrid(p.x, p.y)
          ApproxViewshed.offsets(r, col, row)
        }
      }

}
