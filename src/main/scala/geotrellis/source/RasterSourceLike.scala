package geotrellis.source

import geotrellis._
import geotrellis.feature._
import geotrellis.raster.op._
import geotrellis.statistics.op._
import geotrellis.render.op._

import geotrellis.raster._

import scalaxy.loops._
import scala.collection.mutable

trait RasterSourceLike[+Repr <: RasterSource] 
    extends DataSourceLike[Raster,Raster, Repr]
    with DataSource[Raster,Raster] 
    with local.LocalOpMethods[Repr] 
    with focal.FocalOpMethods[Repr]
    with global.GlobalOpMethods[Repr]
    with zonal.ZonalOpMethods[Repr]
    with zonal.summary.ZonalSummaryOpMethods[Repr]
    with stat.StatOpMethods[Repr] 
    with io.IoOpMethods[Repr] 
    with RenderOpMethods[Repr] { self: Repr =>

  def tiles = self.elements
  def rasterDefinition:Op[RasterDefinition]

  def convergeOp():Op[Raster] =
    tiles.flatMap { ts =>
      if(ts.size == 1) { ts(0) }
      else { 
        (rasterDefinition,logic.Collect(ts)).map { (rd,tileSeq) =>
          TileRaster(tileSeq,rd.re,rd.tileLayout).toArrayRaster
        }
      }
    }

  def global[That](f:Raster=>Raster)
                  (implicit bf:CanBuildSourceFrom[Repr,Raster,That]):That = {
    val tileOps:Op[Seq[Op[Raster]]] =
      (rasterDefinition,logic.Collect(tiles)).map { (rd,tileSeq) =>
        if(rd.isTiled) {
          val r = f(TileRaster(tileSeq.toSeq, rd.re,rd.tileLayout))
          TileRaster.split(r,rd.tileLayout).map(Literal(_))
        } else {
          Seq(f(tileSeq(0)))
        }
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
        if(rd.isTiled) {
          f(TileRaster(tileSeq.toSeq, rd.re,rd.tileLayout)).map { r =>
            TileRaster.split(r,rd.tileLayout).map(Literal(_))
          }
        } else {
          Seq(f(tileSeq(0)))
        }
      }
    // Set into new RasterSource
    val builder = bf.apply(this)
    builder.setOp(tileOps)
    builder.result
  }

  def min():ValueSource[Int] = 
    self.map(_.findMinMax._1)
        .reduce { (m1,m2) =>
          if(isNoData(m1)) m2
          else if(isNoData(m2)) m1
          else math.min(m1,m2)
         }

  def max():ValueSource[Int] = 
    self.map(_.findMinMax._2)
        .reduce { (m1,m2) =>
          if(isNoData(m1)) m2
          else if(isNoData(m2)) m1
          else math.max(m1,m2)
         }

  def minMax():ValueSource[(Int,Int)] = 
    self.map(_.findMinMax)
        .reduce { (mm1,mm2) =>
          val (min1,max1) = mm1
          val (min2,max2) = mm2
          (if(isNoData(min1)) min2
           else if(isNoData(min2)) min1
           else math.min(min1,min2),
           if(isNoData(max1)) max2
           else if(isNoData(max2)) max1
           else math.max(max1,max2)
          )
         }

  def info:ValueSource[process.RasterLayerInfo] = 
    ValueSource(rasterDefinition.flatMap( rd => io.LoadRasterLayerInfo(rd.layerId)))
}
