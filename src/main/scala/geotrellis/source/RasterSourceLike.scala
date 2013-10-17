package geotrellis.source

import geotrellis._
import geotrellis.raster.op._
import geotrellis.statistics.op._

import geotrellis.raster._

trait RasterSourceLike[+Repr <: RasterSource] 
    extends DataSourceLike[Raster,Raster, Repr]
    with DataSource[Raster,Raster] 
    with focal.FocalOpMethods[Repr] 
    with local.LocalOpMethods[Repr] 
    with stat.StatOpMethods[Repr] { self: Repr =>

  def tiles = self.elements
  def rasterDefinition:Op[RasterDefinition]

  def get() =
    (rasterDefinition,logic.Collect(tiles)).map { (rd,tileSeq) =>
      Raster(TileArrayRasterData(tileSeq.toArray, rd.tileLayout),rd.re)
    }

  // def global[That](f:Raster=>Raster)
  //                 (implicit bf:CanBuildSourceFrom[Repr,Raster,That]):That = {
  //   val tileOps:Op[Seq[Op[Raster]]] =
  //     (rasterDefinition,logic.Collect(tiles)).map { (rd,tileSeq) =>
  //       val r = f(Raster(TileArrayRasterData(tileSeq.toArray, rd.tileLayout),rd.re))
  //       r.createTiles(rd.tileLayout).map(Literal(_))
  //     }
  //   // Set into new RasterSource
  //   val builder = bf.apply(this)
  //   builder.setOp(tileOps)
  //   builder.result
  // }

  def combineOp[B,That](rs:RasterSource)
                       (f:(Op[Raster],Op[Raster])=>Op[B])
                       (implicit bf:CanBuildSourceFrom[Repr,B,That]):That = {
    val tileOps:Op[Seq[Op[B]]] =
      (tiles,rs.tiles).map { (ts1,ts2) =>
        ts1.zip(ts2).map { case (t1,t2) => 
          f(t1,t2) 
        }
      }

    val builder = bf.apply(this)
    builder.setOp(tileOps)
    builder.result
  }

  def combine[That](rs:RasterSource)
                   (f:(Int,Int)=>Int)
                   (implicit bf:CanBuildSourceFrom[Repr,Raster,That]):That = {
    val tileOps =
      (tiles,rs.tiles).map { (ts1,ts2) =>
        for((t1,t2) <- ts1.zip(ts2)) yield {
          (t1,t2).map { (r1,r2) =>
            r1.combine(r2)(f)
          }
        }
      }

    val builder = bf.apply(this)
    builder.setOp(tileOps)
    builder.result
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
    builder.result
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
    builder.result
  }
}
