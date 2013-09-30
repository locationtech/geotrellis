package geotrellis.source

import geotrellis._
import geotrellis.raster.op._
import geotrellis.statistics.op._
import geotrellis.data._
import geotrellis.raster._
import geotrellis.statistics._

trait RasterSourceLike[+Repr <: RasterSource] 
    extends DataSourceLike[Raster,Raster, Repr]
    with DataSource[Raster,Raster] { self: Repr =>

  def tiles = self.elements
  def rasterDefinition:Op[RasterDefinition]

  def get()(implicit mf:Manifest[Raster]) = {
    rasterDefinition flatMap { rd =>
      val re = rd.re
      logic.Collect(rd.tiles).map(s => Raster(TileArrayRasterData(s.toArray, rd.tileLayout, re),re))
    }}
  
  // Methods w/ transformations that retain this source type.
  def localAdd(i: Int) = this mapOp(local.Add(_, i))
  def localSubtract(i: Int):RasterSource = this mapOp(local.Subtract(_, i))

  // Methods that return a local source but can act on any type.
  def histogram():HistogramDS = this mapOp(stat.GetHistogram(_))

  // Methods that act on a local source.

  def combine[That](rs:RasterSource)(f:(Int,Int)=>Int)(implicit bf:CanBuildSourceFrom[Repr,Raster,That]):That = {
    // Check that extents are the same
    // ...
    val tileOps =
      for(ts1 <- tiles;
          ts2 <- rs.tiles;
          (t1,t2) <- ts1.zip(ts2)) yield {
        for(r1 <- t1;
            r2 <- t2) yield {
          r1.combine(r2)(f)
        }
      }
    val builder = bf.apply(this)
    builder.setOp(tileOps)
    builder.result()
  }

  def combineDouble[That](rs:RasterSource)(f:(Double,Double)=>Double)(implicit bf:CanBuildSourceFrom[Repr,Raster,That]):That = {
    // Check that extents are the same
    // ...
    val tileOps = 
      for(ts1 <- tiles;
          ts2 <- rs.tiles;
          (t1,t2) <- ts1.zip(ts2)) yield {
        for(r1 <- t1;
            r2 <- t2) yield {
          r1.combineDouble(r2)(f)
        }
      }
    val builder = bf.apply(this)
    builder.setOp(tileOps)
    builder.result()
  }

  def dualCombine[That](rs:RasterSource)(fInt:(Int,Int)=>Int)(fDouble:(Double,Double)=>Double)(implicit bf:CanBuildSourceFrom[Repr,Raster,That]):That = {
    val tileOps =
      for(ts1 <- tiles;
          ts2 <- rs.tiles;
          (t1,t2) <- ts1.zip(ts2)) yield {
        for(r1 <- t1;
            r2 <- t2) yield {
          r1.dualCombine(r2)(fInt)(fDouble)
        }
      }
    val builder = bf.apply(this)
    builder.setOp(tileOps)
    builder.result()
  }

}
