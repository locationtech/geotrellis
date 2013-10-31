package geotrellis.source

import geotrellis._
import geotrellis.feature._
import geotrellis.raster.op._
import geotrellis.statistics.op._

import geotrellis.raster._

import scalaxy.loops._
import scala.collection.mutable

trait RasterSourceLike[+Repr <: RasterSource] 
    extends DataSourceLike[Raster,Raster, Repr]
    with DataSource[Raster,Raster] 
    with local.LocalOpMethods[Repr] 
    with focal.FocalOpMethods[Repr]
    with global.GlobalOpMethods[Repr]
    with zonal.summary.ZonalSummaryOpMethods[Repr]
    with stat.StatOpMethods[Repr] { self: Repr =>

  def tiles = self.elements
  def rasterDefinition:Op[RasterDefinition]

  def get():Op[Raster] =
    (rasterDefinition,logic.Collect(tiles)).map { (rd,tileSeq) =>
      TileRaster(tileSeq,rd.re,rd.tileLayout).toArrayRaster
    }

  def global[That](f:Raster=>Raster)
                  (implicit bf:CanBuildSourceFrom[Repr,Raster,That]):That = {
    val tileOps:Op[Seq[Op[Raster]]] =
      (rasterDefinition,logic.Collect(tiles)).map { (rd,tileSeq) =>
        val r = f(TileRaster(tileSeq.toSeq, rd.re,rd.tileLayout))
        TileRaster.split(r,rd.tileLayout).map(Literal(_))
      }
    // Set into new RasterSource
    val builder = bf.apply(this)
    builder.setOp(tileOps)
    builder.result
  }

  def globalOp[T,That](f:Raster=>Op[Raster])
                    (implicit bf:CanBuildSourceFrom[Repr,Raster,That]):That = {
    val tileOps:Op[Seq[Op[Raster]]] =
      (rasterDefinition,logic.Collect(tiles)).flatMap { (rd,tileSeq) =>
        f(TileRaster(tileSeq.toSeq, rd.re,rd.tileLayout)).map { r =>
          TileRaster.split(r,rd.tileLayout).map(Literal(_))
        }
      }
    // Set into new RasterSource
    val builder = bf.apply(this)
    builder.setOp(tileOps)
    builder.result
  }

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

  def combineOp[B,That](rs:Seq[RasterSource])
                       (f:Seq[Op[Raster]]=>Op[B])
                       (implicit bf:CanBuildSourceFrom[Repr,B,That]):That = {
    val tileOps:Op[Seq[Op[B]]] =
      (tiles,logic.Collect(rs.map(_.tiles))).map { (thisTiles,restTiles) =>
        (thisTiles +: restTiles).transpose.map(f)
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
            r1.dualCombine(r2)(f)((z1:Double, z2:Double) => i2d(f(d2i(z1), d2i(z2))))
          }
        }
      }

    val builder = bf.apply(this)
    builder.setOp(tileOps)
    builder.result
  }


  def combineDouble[That](rs:RasterSource)
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

  def dualCombine[That](rs:RasterSource)
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

  def min():ValueSource[Int] = 
    self.map(_.findMinMax._1)
        .reduce { (m1,m2) =>
          if(m1 == NODATA) m2
          else if(m2 == NODATA) m1
          else math.min(m1,m2)
         }

  def max():ValueSource[Int] = 
    self.map(_.findMinMax._2)
        .reduce { (m1,m2) =>
          if(m1 == NODATA) m2
          else if(m2 == NODATA) m1
          else math.max(m1,m2)
         }

  def minMax():ValueSource[(Int,Int)] = 
    self.map(_.findMinMax)
        .reduce { (mm1,mm2) =>
          val (min1,max1) = mm1
          val (min2,max2) = mm2
          (if(min1 == NODATA) min2
           else if(min2 == NODATA) min1
           else math.min(min1,min2),
           if(max1 == NODATA) max2
           else if(max2 == NODATA) max1
           else math.max(max1,max2)
          )
         }
}

abstract sealed trait TileIntersection

case class PartialTileIntersection[D](tile:Raster,intersections:List[Polygon[D]]) extends TileIntersection
case class FullTileIntersection(tile:Raster) extends TileIntersection
