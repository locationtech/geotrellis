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

package geotrellis.engine.op.global

import geotrellis.engine._
import geotrellis.raster._
import geotrellis.raster.op.global._
import geotrellis.vector._

trait GlobalRasterSourceMethods extends RasterSourceMethods {
  def convolve(kernel: Kernel) =
    rasterSource.global(Convolve(_, kernel))

  def costDistance(points: Seq[(Int, Int)]) = 
    rasterSource.global(CostDistance(_, points))

  def rescale(newMin: Int, newMax: Int) =
    rasterSource.global(_.rescale(newMin, newMax))

  def rescale(newMin: Double, newMax: Double) =
    rasterSource.global(_.rescale(newMin, newMax))

  def toVector() = 
    rasterSource.converge.mapOp { tileOp =>
      (tileOp, rasterSource.rasterDefinition).map { (tile, rd) => 
        tile.toVector(rd.rasterExtent.extent) 
      }
    }

  def regionGroup(options: RegionGroupOptions = RegionGroupOptions.default) =
    rasterSource.converge.map(_.regionGroup(options))

  def verticalFlip() =
    rasterSource.global(VerticalFlip(_))

  def viewshed(p: Point, exact: Boolean = false) =
    if(exact)
      rasterSource.globalOp { r => 
        rasterSource.rasterDefinition.map { rd =>
          val (col, row) = rd.rasterExtent.mapToGrid(p.x, p.y)
          Viewshed(r, col, row) 
        }
      }
    else
      rasterSource.globalOp { r => 
        rasterSource.rasterDefinition.map { rd =>
          val (col, row) = rd.rasterExtent.mapToGrid(p.x, p.y)
          ApproxViewshed(r, col, row)
        }
      }

  def viewshedOffsets(p: Point, exact: Boolean = false) =
    if(exact)
      rasterSource.globalOp { r => 
        rasterSource.rasterDefinition.map { rd =>
          val (col, row) = rd.rasterExtent.mapToGrid(p.x, p.y)
          Viewshed.offsets(r, col, row) 
        }
      }
    else
      rasterSource.globalOp { r => 
        rasterSource.rasterDefinition.map { rd =>
          val (col, row) = rd.rasterExtent.mapToGrid(p.x, p.y)
          ApproxViewshed.offsets(r, col, row)
        }
      }
}
