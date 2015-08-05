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

package geotrellis.engine.op.local

import geotrellis.engine._
import geotrellis.raster._
import geotrellis.raster.op.local._
import geotrellis.raster.rasterize.Rasterizer
import geotrellis.vector.Geometry

trait LocalRasterSourceMethods
  extends RasterSourceMethods
     with LocalMapRasterSourceMethods
     with AddRasterSourceMethods
     with SubtractRasterSourceMethods
     with MultiplyRasterSourceMethods
     with DivideRasterSourceMethods
     with AndRasterSourceMethods
     with OrRasterSourceMethods
     with XorRasterSourceMethods
     with MinRasterSourceMethods
     with MaxRasterSourceMethods
     with PowRasterSourceMethods
     with EqualRasterSourceMethods
     with UnequalRasterSourceMethods
     with GreaterRasterSourceMethods
     with LessRasterSourceMethods
     with GreaterOrEqualRasterSourceMethods
     with LessOrEqualRasterSourceMethods
     with ConditionalRasterSourceMethods
     with MajorityRasterSourceMethods
     with MinorityRasterSourceMethods {

  def localCombine[That](rs: RasterSource)
                        (f: (Int, Int)=>Int): RasterSource = {
    val tileOps =
      (rasterSource.tiles, rs.tiles).map { (ts1, ts2) =>
        for((t1, t2) <- ts1.zip(ts2)) yield {
          (t1, t2).map { (r1, r2) =>
            r1.dualCombine(r2)(f)((z1: Double, z2: Double) => i2d(f(d2i(z1), d2i(z2))))
          }
        }
      }

    RasterSource(rasterSource.rasterDefinition, tileOps)
  }


  def localCombineDouble[That](rs: RasterSource)
                              (f: (Double, Double)=>Double): RasterSource = {
    val tileOps = 
      (rasterSource.tiles, rs.tiles).map { (ts1, ts2) =>
        for((t1, t2) <- ts1.zip(ts2)) yield {
          (t1, t2).map { (r1, r2) =>
            r1.dualCombine(r2)((z1: Int, z2: Int)=>d2i(f(i2d(z1), i2d(z2))))(f)
          }
        }
      }
    RasterSource(rasterSource.rasterDefinition, tileOps)
  }

  def localDualCombine[That](rs: RasterSource)
                       (fInt: (Int, Int)=>Int)
                       (fDouble: (Double, Double)=>Double): RasterSource = {
    val tileOps =
      (rasterSource.tiles, rs.tiles).map { (ts1, ts2) =>
        for((t1, t2) <- ts1.zip(ts2)) yield {
          (t1, t2).map { (r1, r2) =>
            r1.dualCombine(r2)(fInt)(fDouble)
          }
        }
      }
    RasterSource(rasterSource.rasterDefinition, tileOps)
  }

  /** Get the negation of this raster source. Will convert double values into integers. */
  def localNot(): RasterSource = rasterSource.mapTile(Not(_), "Not[Raster]")

  /** Get the negation of this raster source. Will convert double values into integers. */
  def unary_~(): RasterSource = localNot()

  /** Negate (multiply by -1) each value in a raster. */
  def localNegate(): RasterSource = rasterSource.mapTile(Negate(_), "Negate[Raster]")
  /** Negate (multiply by -1) each value in a raster. */
  def unary_-(): RasterSource = localNegate()

  /** Takes the absolute value of each raster cell value. */
  def localAbs(): RasterSource = rasterSource.mapTile(Abs(_), "Abs")

  /** Takes the Ceiling of each raster cell value. */
  def localCeil(): RasterSource = rasterSource.mapTile(Ceil(_), "Ceil")

  /** Takes the Floor of each raster cell value. */
  def localFloor(): RasterSource = rasterSource.mapTile(Floor(_), "Floor")

  /** Computes the Log of a Raster. */
  def localLog(): RasterSource = rasterSource.mapTile(Log(_), "Log")

  /** Takes the Log base 10 of each raster cell value. */
  def localLog10(): RasterSource = rasterSource.mapTile(Log10(_), "Log10")

  /** Round the values of a Raster. */
  def localRound(): RasterSource = rasterSource.mapTile(Round(_), "Round")

  /** Take the square root each value in a raster. */
  def localSqrt(): RasterSource = rasterSource.mapTile(Sqrt(_), "Sqrt")

  /** Maps an integer typed Raster to 1 if the cell value is not NODATA, otherwise 0. */
  def localDefined(): RasterSource = rasterSource.mapTile(Defined(_), "Defined")

  /** Maps an integer typed Raster to 0 if the cell value is not NODATA, otherwise 1. */
  def localUndefined(): RasterSource = rasterSource.mapTile(Undefined(_), "Undefined")

  /** Masks this raster based on cell values of the second raster. See [[Mask]]. */
  def localMask(rs: RasterSource, readMask: Int, writeMask: Int): RasterSource = 
    rasterSource.combineTile(rs, "localMask")(Mask(_, _, readMask, writeMask))

  /** InverseMasks this raster based on cell values of the second raster. See [[InverseMask]]. */
  def localInverseMask(rs: RasterSource, readMask: Int, writeMask: Int): RasterSource = 
    rasterSource.combineTile(rs, "localMask")(InverseMask(_, _, readMask, writeMask))

  /** Takes the mean of the values of each cell in the set of rasters. */
  def localMean(rss: Seq[RasterSource]): RasterSource = 
    rasterSource.combineTile(rss, "Mean")(Mean(_))

  /** Takes the mean of the values of each cell in the set of rasters. */
  def localMean(rss: RasterSource*)(implicit d: DI): RasterSource = 
    localMean(rss)

 /** Gives the count of unique values at each location in a set of Rasters.*/
  def localVariety(rss: Seq[RasterSource]): RasterSource = 
    rasterSource.combineTile(rss, "Variety")(Variety(_))

 /** Gives the count of unique values at each location in a set of Rasters.*/
  def localVariety(rss: RasterSource*)(implicit d: DI): RasterSource = 
    localVariety(rss)

  /** Takes the sine of each raster cell value. */
  def localSin(): RasterSource = rasterSource.mapTile(Sin(_), "Sin")

  /** Takes the cosine of each raster cell value. */
  def localCos(): RasterSource = rasterSource.mapTile(Cos(_), "Cos")

  /** Takes the tangent of each raster cell value. */
  def localTan(): RasterSource = rasterSource.mapTile(Tan(_), "Tan")

  /** Takes the sineh of each raster cell value. */
  def localSinh(): RasterSource = rasterSource.mapTile(Sinh(_), "Sinh")

  /** Takes the cosineh of each raster cell value. */
  def localCosh(): RasterSource = rasterSource.mapTile(Cosh(_), "Cosh")

  /** Takes the tangenth of each raster cell value. */
  def localTanh(): RasterSource = rasterSource.mapTile(Tanh(_), "Tanh")

  /** Takes the arc sine of each raster cell value. */
  def localAsin(): RasterSource = rasterSource.mapTile(Asin(_), "Asin")

  /** Takes the arc cosine of each raster cell value. */
  def localAcos(): RasterSource = rasterSource.mapTile(Acos(_), "Acos")

  /** Takes the arc tangent of each raster cell value. */
  def localAtan(): RasterSource = rasterSource.mapTile(Atan(_), "Atan")

  /** Takes the arc tangent 2 of each raster cell value. */
  def localAtan2(rs: RasterSource): RasterSource = 
    rasterSource.combineTile(rs, "Atan2")(Atan2(_, _))

  /** Assigns to each cell the value within the given rasters that is the nth min */
  def localMinN(n: Int, rss: Seq[RasterSource]): RasterSource =
    rasterSource.combineTile(rss)(MinN(n, _))

  /** Assigns to each cell the value within the given rasters that is the nth min */
  def localMinN(n: Int, rss: RasterSource*)(implicit d: DI): RasterSource =
    localMinN(n, rss)

  /** Assigns to each cell the value within the given rasters that is the nth max */
  def localMaxN(n: Int, rss: Seq[RasterSource]): RasterSource =
    rasterSource.combineTile(rss)(MaxN(n, _))

  /** Assigns to each cell the value within the given rasters that is the nth max */
  def localMaxN(n: Int, rss: RasterSource*)(implicit d: DI): RasterSource =
    localMaxN(n, rss)

  /** Masks this raster by the given Geometry. */
  def mask(geom: Geometry): RasterSource = {
    mask(Seq(geom))
  }

  /** Masks this raster by the given Geometry. */
  def mask(geoms: Iterable[Geometry]): RasterSource = {
    rasterSource.mapRaster { case Raster(tile, extent) =>
      val (cols, rows) = tile.dimensions
      val re = RasterExtent(extent, cols, rows)
      val result = ArrayTile.empty(tile.cellType, cols, rows)
      for(g <- geoms) {
        if(tile.cellType.isFloatingPoint) {
          Rasterizer.foreachCellByGeometry(g, re) { (col: Int, row: Int) =>
              result.setDouble(col, row, tile.getDouble(col, row))
          }
        } else {
          Rasterizer.foreachCellByGeometry(g, re) { (col: Int, row: Int) =>
              result.set(col, row, tile.get(col, row))
          }
        }
      }
      result:Tile
    }
  }

}
