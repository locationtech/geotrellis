/**************************************************************************
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
 **************************************************************************/

package geotrellis.raster.op.local

import geotrellis._
import geotrellis.raster._
import geotrellis.raster.op.ConvertType
import geotrellis.source._
import geotrellis.data.geojson.GeoJsonReader
import geotrellis.feature.rasterize.{Rasterizer, Callback}
import geotrellis.feature.Geometry

trait LocalOpMethods[+Repr <: RasterSource] 
  extends LocalMapOpMethods[Repr]
     with AddOpMethods[Repr]
     with SubtractOpMethods[Repr]
     with MultiplyOpMethods[Repr]
     with DivideOpMethods[Repr] 
     with AndOpMethods[Repr] 
     with OrOpMethods[Repr] 
     with XorOpMethods[Repr] 
     with MinOpMethods[Repr] 
     with MaxOpMethods[Repr] 
     with PowOpMethods[Repr] 
     with EqualOpMethods[Repr] 
     with UnequalOpMethods[Repr] 
     with GreaterOpMethods[Repr]
     with LessOpMethods[Repr] 
     with GreaterOrEqualOpMethods[Repr] 
     with LessOrEqualOpMethods[Repr] 
     with ConditionalOpMethods[Repr] 
     with MajorityOpMethods[Repr] 
     with MinorityOpMethods[Repr] 
     with VarietyOpMethods[Repr] { self: Repr =>

  def localCombine[That](rs:RasterSource)
                   (f:(Int,Int)=>Int)
                   (implicit bf:CanBuildSourceFrom[Repr,Raster,That]):That = {
    val tileOps =
      (tiles,rs.tiles).map { (ts1,ts2) =>
        for((t1,t2) <- ts1.zip(ts2)) yield {
          (t1,t2).map { (r1,r2) =>
            r1.dualCombine(r2)(f)((z1:Double, z2:Double) => i2d(f(d2i(z1), d2i(z2))))
          }
        }
      }

    val builder = bf.apply(this)
    builder.setOp(tileOps)
    builder.result
  }


  def localCombineDouble[That](rs:RasterSource)
                             (f:(Double,Double)=>Double)
                             (implicit bf:CanBuildSourceFrom[Repr,Raster,That]):That = {
    val tileOps = 
      (tiles,rs.tiles).map { (ts1,ts2) =>
        for((t1,t2) <- ts1.zip(ts2)) yield {
          (t1,t2).map { (r1,r2) =>
            r1.dualCombine(r2)((z1:Int,z2:Int)=>d2i(f(i2d(z1), i2d(z2))))(f)
          }
        }
      }
    val builder = bf.apply(this)
    builder.setOp(tileOps)
    builder.result
  }

  def localDualCombine[That](rs:RasterSource)
                       (fInt:(Int,Int)=>Int)
                       (fDouble:(Double,Double)=>Double)
                       (implicit bf:CanBuildSourceFrom[Repr,Raster,That]):That = {
    val tileOps =
      (tiles,rs.tiles).map { (ts1,ts2) =>
        for((t1,t2) <- ts1.zip(ts2)) yield {
          (t1,t2).map { (r1,r2) =>
            r1.dualCombine(r2)(fInt)(fDouble)
          }
        }
      }
    val builder = bf.apply(this)
    builder.setOp(tileOps)
    builder.result
  }

  /** Get the negation of this raster source. Will convert double values into integers. */
  def localNot(): RasterSource = map(Not(_), "Not[Raster]")

  /** Get the negation of this raster source. Will convert double values into integers. */
  def unary_~(): RasterSource = localNot()

  /** Negate (multiply by -1) each value in a raster. */
  def localNegate(): RasterSource = map(Negate(_), "Negate[Raster]")
  /** Negate (multiply by -1) each value in a raster. */
  def unary_-(): RasterSource = localNegate()

  /** Takes the absolute value of each raster cell value. */
  def localAbs(): RasterSource = map(Abs(_), "Abs")

  /** Takes the Ceiling of each raster cell value. */
  def localCeil(): RasterSource = map(Ceil(_), "Ceil")

  /** Takes the Floor of each raster cell value. */
  def localFloor(): RasterSource = map(Floor(_), "Floor")

  /** Computes the Log of a Raster. */
  def localLog(): RasterSource = map(Log(_), "Log")

  /** Takes the Log base 10 of each raster cell value. */
  def localLog10(): RasterSource = map(Log10(_), "Log10")

  /** Round the values of a Raster. */
  def localRound(): RasterSource = map(Round(_), "Round")

  /** Take the square root each value in a raster. */
  def localSqrt(): RasterSource = map(Sqrt(_), "Sqrt")

  /** Maps an integer typed Raster to 1 if the cell value is not NODATA, otherwise 0. */
  def localDefined(): RasterSource = map(Defined(_), "Defined")

  /** Maps an integer typed Raster to 0 if the cell value is not NODATA, otherwise 1. */
  def localUndefined(): RasterSource = map(Undefined(_), "Undefined")

  /** Masks this raster based on cell values of the second raster. See [[Mask]]. */
  def localMask(rs:RasterSource,readMask:Int,writeMask:Int): RasterSource = 
    combine(rs, "localMask")(Mask(_,_,readMask,writeMask))

  /** InverseMasks this raster based on cell values of the second raster. See [[InverseMask]]. */
  def localInverseMask(rs:RasterSource,readMask:Int,writeMask:Int): RasterSource = 
    combine(rs, "localMask")(InverseMask(_,_,readMask,writeMask))

  /** Takes the mean of the values of each cell in the set of rasters. */
  def localMean(rss: Seq[RasterSource]): RasterSource = 
    combine(rss, "Mean")(Mean(_))

  /** Takes the mean of the values of each cell in the set of rasters. */
  def localMean(rss:RasterSource*)(implicit d:DI): RasterSource = 
    localMean(rss)

  /** Takes the sine of each raster cell value. */
  def localSin(): RasterSource = map(Sin(_), "Sin")

  /** Takes the cosine of each raster cell value. */
  def localCos(): RasterSource = map(Cos(_), "Cos")

  /** Takes the tangent of each raster cell value. */
  def localTan(): RasterSource = map(Tan(_), "Tan")

  /** Takes the sineh of each raster cell value. */
  def localSinh(): RasterSource = map(Sinh(_), "Sinh")

  /** Takes the cosineh of each raster cell value. */
  def localCosh(): RasterSource = map(Cosh(_), "Cosh")

  /** Takes the tangenth of each raster cell value. */
  def localTanh(): RasterSource = map(Tanh(_), "Tanh")

  /** Takes the arc sine of each raster cell value. */
  def localAsin(): RasterSource = map(Asin(_), "Asin")

  /** Takes the arc cosine of each raster cell value. */
  def localAcos(): RasterSource = map(Acos(_), "Acos")

  /** Takes the arc tangent of each raster cell value. */
  def localAtan(): RasterSource = map(Atan(_), "Atan")

  /** Takes the arc tangent 2 of each raster cell value. */
  def localAtan2(rs: RasterSource): RasterSource = combine(rs, "Atan2")(Atan2(_,_))

  /** Masks this raster by the given GeoJSON. */
  def mask(geoJson: String): RasterSource =
    GeoJsonReader.parse(geoJson) match {
      case Some(geomArray) => mask(geomArray)
      case None => sys.error(s"Invalid GeoJSON: $geoJson")
    }

  /** Masks this raster by the given GeoJSON. */
  def mask[T](geom: Geometry[T]): RasterSource = 
    mask(Seq(geom))

  /** Masks this raster by the given GeoJSON. */
  def mask[T](geoms: Iterable[Geometry[T]]): RasterSource =
    map { tile =>
      val re = tile.rasterExtent
      val data = RasterData.emptyByType(tile.rasterType, re.cols, re.rows)
      for(g <- geoms) {
        if(tile.isFloat) {
          Rasterizer.foreachCellByFeature(g, re)(new Callback[Geometry,T] {
            def apply(col: Int, row: Int, g: Geometry[T]) =
              data.setDouble(col,row,tile.getDouble(col,row))
          })
        } else {
          Rasterizer.foreachCellByFeature(g, re)(new Callback[Geometry,T] {
            def apply(col: Int, row: Int, g: Geometry[T]) =
              data.set(col,row,tile.get(col,row))
          })
        }
      }
      Raster(data,re)
    }
}
