package geotrellis.source

import geotrellis._
import geotrellis.feature._
import geotrellis.raster.op._
import geotrellis.statistics.op._

import geotrellis.raster._

import scalaxy.loops._
import scala.collection.mutable

trait RasterDataSourceLike[+Repr <: RasterDataSource] 
    extends DataSourceLike[Raster,Raster, Repr]
    with DataSource[Raster,Raster] 
    with focal.FocalOpMethods[Repr] 
    with local.LocalOpMethods[Repr] 
    with zonal.summary.ZonalSummaryOpMethods[Repr]
    with stat.StatOpMethods[Repr] { self: Repr =>

  def tiles = self.elements
  def rasterDefinition:Op[RasterDefinition]

  def get() =
    (rasterDefinition,logic.Collect(tiles)).map { (rd,tileSeq) =>
      TileRaster(tileSeq,rd.re,rd.tileLayout).toArrayRaster
    }

  def global[That](f:RasterLike=>Raster)
                  (implicit bf:CanBuildSourceFrom[Repr,Raster,That]):That = {
    val tileOps:Op[Seq[Op[Raster]]] =
      (rasterDefinition,logic.Collect(tiles)).map { (rd,tileSeq) =>
        val r = f(TileRaster(tileSeq.toSeq, rd.re,rd.tileLayout))
        TileRaster.split(r,rd.tileLayout).map(Literal(_))
      }
    // Set into new RasterDataSource
    val builder = bf.apply(this)
    builder.setOp(tileOps)
    builder.result
  }

  def combineOp[B,That](rs:RasterDataSource)
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

  def combine[That](rs:RasterDataSource)
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


  def combineDouble[That](rs:RasterDataSource)(f:(Double,Double)=>Double)(implicit bf:CanBuildSourceFrom[Repr,Raster,That]):That = {
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

  def dualCombine[That](rs:RasterDataSource)(fInt:(Int,Int)=>Int)(fDouble:(Double,Double)=>Double)(implicit bf:CanBuildSourceFrom[Repr,Raster,That]):That = {
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

  def filterTiles(p:Op[feature.Polygon[_]]):Op[Seq[Op[TileIntersection]]] = {
    (rasterDefinition,tiles,p).map { (rd,tiles,p) =>
      val rl = rd.tileLayout.getResolutionLayout(rd.re)
      val tileCols = rd.tileLayout.tileCols
      val tileRows = rd.tileLayout.tileRows
      val filtered = mutable.ListBuffer[Op[TileIntersection]]()
      for(col <- 0 until tileCols optimized) {
        for(row <- 0 until tileRows optimized) {
          val tilePoly = 
            rl.getRasterExtent(col,row)
              .extent
              .asFeature()
              .geom

          if(p.geom.contains(tilePoly)) {
            filtered += tiles(row*tileCols + col).map(FullTileIntersection(_))
          } else {
            val intersections = tilePoly.intersection(p.geom).asPolygonSet.map(Polygon(_,0))
            if(!intersections.isEmpty) {
              filtered += tiles(row*tileCols + col).map(PartialTileIntersection(_,intersections))
            }
          }
        }
      }
      filtered.toSeq
    }
  }

  def mapIntersecting[B,That,D](p:Op[feature.Polygon[D]])(handleTileIntersection:TileIntersection=>B)(implicit bf:CanBuildSourceFrom[Repr,B,That]):That = {
    val builder = bf.apply(this)
    val newOp = 
      filterTiles(p).map { filteredTiles =>
        filteredTiles.map { tileIntersectionOp =>
          tileIntersectionOp.map(handleTileIntersection(_))
        }
      }
    builder.setOp(newOp)
    val result = builder.result()
    result
  }
}

abstract sealed trait TileIntersection

case class PartialTileIntersection[D](tile:Raster,intersections:List[Polygon[D]]) extends TileIntersection
case class FullTileIntersection(tile:Raster) extends TileIntersection
