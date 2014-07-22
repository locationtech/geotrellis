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

package geotrellis.raster.op.local

import geotrellis.raster._

import scala.annotation.tailrec

class TileReducer(handle: (Int, Int)=>Int)(handleDouble: (Double, Double)=>Double) {
  // This class benchmarks fast, if you change it be sure to compare performance.
  def apply(seq: Traversable[Tile]): Tile =
    apply(seq.toList)

  def apply(list: List[Tile]): Tile =
    handleTiles(list)

  @tailrec final def reduce(d: Tile, rasters: List[Tile]): Tile = {
    rasters match {
      case Nil => d
      case r :: rs => if (r.cellType.isFloatingPoint) {
        reduceDouble(d.combineDouble(r)(handleDouble), rs)
      } else {
        reduce(d.combine(r)(handle), rs)
      }
    }
  }

  @tailrec final def reduceDouble(d: Tile, rasters: List[Tile]): Tile = {
    rasters match {
      case Nil => d
      case r :: rs => reduceDouble(d.combineDouble(r)(handleDouble), rs)
    }
  }

  def handleTiles(rasters: List[Tile]) = {
    val (r :: rs) = rasters

    if (r.cellType.isFloatingPoint) {
      reduceDouble(r, rs)
    } else {
      reduce(r, rs)
    }
  }
}
