/*
 * Copyright (c) 2014 Azavea.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE - 2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.raster

import geotrellis.raster._
import geotrellis.raster.op.local._
import geotrellis.engine._

/** This class gives the ability to apply a local operation
  * that reduces a sequence of rasters to a set of rasters
  * that are loaded in parallel. So if you wanted to group
  * the raster loading by loading some number of RasterSources
  * simultaneously, use this class to group the RasterSources.
  */
case class RasterSourceSeq(seq: Seq[RasterSource]) {
  val rasterDefinition = seq.head.rasterDefinition

  def applyOp(f: Seq[Op[Tile]]=>Op[Tile]) = {
    val builder = new RasterSourceBuilder()
    builder.setRasterDefinition(rasterDefinition)
    builder.setOp {
      seq.map(_.tiles).mapOps(_.transpose.map(f))
    }
    builder.result
  }

  // /** Adds all the rasters in the sequence */
  def localAdd(): RasterSource = 
    applyOp { tileOps =>
      logic.Collect(tileOps).map { tiles: Seq[Tile] => Add(tiles) }
    }

  /** Takes the difference of the rasters in the sequence from left to right */
  def difference() = localSubtract

  /** Takes the difference of the rasters in the sequence from left to right */
  def localSubtract() = 
    applyOp { tileOps =>
      logic.Collect(tileOps).map { tiles: Seq[Tile] => Subtract(tiles) }
    }

  /** Takes the product of the rasters in the sequence */
  def product() = localMultiply

  /** Takes the product of the rasters in the sequence */
  def localMultiply() = 
    applyOp { tileOps =>
      logic.Collect(tileOps).map { tiles: Seq[Tile] => Multiply(tiles) }
    }

  /** Divides the rasters in the sequence from left to right */
  def localDivide() = 
    applyOp { tileOps =>
      logic.Collect(tileOps).map { tiles: Seq[Tile] => Divide(tiles) }
    }

  /** Takes the max of each cell value */
  def max() = 
    applyOp { tileOps =>
      logic.Collect(tileOps).map { tiles: Seq[Tile] => Max(tiles) }
    }

  /** Takes the min of each cell value */
  def min() = 
    applyOp { tileOps =>
      logic.Collect(tileOps).map { tiles: Seq[Tile] => Min(tiles) }
    }

  /** Takes the logical And of each cell value */
  def and() = 
    applyOp { tileOps =>
      logic.Collect(tileOps).map { tiles: Seq[Tile] => And(tiles) }
    }

  /** Takes the logical Or of each cell value */
  def or() = 
    applyOp { tileOps =>
      logic.Collect(tileOps).map { tiles: Seq[Tile] => Or(tiles) }
    }

  /** Takes the logical Xor of each cell value */
  def xor() = 
    applyOp { tileOps =>
      logic.Collect(tileOps).map { tiles: Seq[Tile] => Xor(tiles) }
    }

  /** Raises each cell value to the power of the next raster, from left to right */
  def exponentiate() = 
    applyOp { tileOps =>
      logic.Collect(tileOps).map { tiles: Seq[Tile] => Pow(tiles) }
    }
}
