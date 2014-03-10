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
     with MinNOpMethods[Repr]
     with MaxNOpMethods[Repr]
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
  def localNot() = mapOp(Not(_))
  /** Get the negation of this raster source. Will convert double values into integers. */
  def unary_~() = localNot()

  /** Negate (multiply by -1) each value in a raster. */
  def localNegate() = mapOp(Negate(_))
  /** Negate (multiply by -1) each value in a raster. */
  def unary_-() = localNegate()

  /** Takes the absolute value of each raster cell value. */
  def localAbs() = mapOp(Abs(_))

  /** Takes the Ceiling of each raster cell value. */
  def localCeil() = mapOp(Ceil(_))

  /** Takes the Floor of each raster cell value. */
  def localFloor() = mapOp(Floor(_))

  /** Computes the Log of a Raster. */
  def localLog() = mapOp(Log(_))

  /** Takes the Log base 10 of each raster cell value. */
  def localLog10() = mapOp(Log10(_))

  /** Round the values of a Raster. */
  def localRound() = mapOp(Round(_))

  /** Take the square root each value in a raster. */
  def localSqrt() = mapOp(Sqrt(_))

  /** Maps an integer typed Raster to 1 if the cell value is not NODATA, otherwise 0. */
  def localDefined() = mapOp(Defined(_))

  /** Maps an integer typed Raster to 0 if the cell value is not NODATA, otherwise 1. */
  def localUndefined() = mapOp(Undefined(_))

  /** Masks this raster based on cell values of the second raster. See [[Mask]]. */
  def localMask(rs:RasterSource,readMask:Int,writeMask:Int) = 
    combineOp(rs)(Mask(_,_,readMask,writeMask))

  /** InverseMasks this raster based on cell values of the second raster. See [[InverseMask]]. */
  def localInverseMask(rs:RasterSource,readMask:Int,writeMask:Int) = 
    combineOp(rs)(InverseMask(_,_,readMask,writeMask))

  /** Takes the mean of the values of each cell in the set of rasters. */
  def localMean(rss:Seq[RasterSource]):RasterSource = 
    combineOp(rss)(Mean(_))

  /** Takes the mean of the values of each cell in the set of rasters. */
  def localMean(rss:RasterSource*)(implicit d:DI):RasterSource = 
    localMean(rss)

  /** Takes the sine of each raster cell value. */
  def localSin() = mapOp(Sin(_))

  /** Takes the cosine of each raster cell value. */
  def localCos() = mapOp(Cos(_))

  /** Takes the tangent of each raster cell value. */
  def localTan() = mapOp(Tan(_))

  /** Takes the sineh of each raster cell value. */
  def localSinh() = mapOp(Sinh(_))

  /** Takes the cosineh of each raster cell value. */
  def localCosh() = mapOp(Cosh(_))

  /** Takes the tangenth of each raster cell value. */
  def localTanh() = mapOp(Tanh(_))

  /** Takes the arc sine of each raster cell value. */
  def localAsin() = mapOp(Asin(_))

  /** Takes the arc cosine of each raster cell value. */
  def localAcos() = mapOp(Acos(_))

  /** Takes the arc tangent of each raster cell value. */
  def localAtan() = mapOp(Atan(_))

  /** Takes the arc tangent 2 of each raster cell value. */
  def localAtan2(rs: RasterSource) = combineOp(rs)(Atan2(_,_))

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
