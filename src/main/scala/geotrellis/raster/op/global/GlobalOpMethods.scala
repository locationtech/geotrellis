package geotrellis.raster.op.global

import geotrellis._
import geotrellis.raster._
import geotrellis.source._

trait GlobalOpMethods[+Repr <: RasterDataSource] { self: Repr =>
  def min():ValueDataSource[Int] = 
    self.map(_.findMinMax._1)
        .reduce { (m1,m2) =>
          if(m1 == NODATA) m2
          else if(m2 == NODATA) m1
          else math.min(m1,m2)
         }

  def max():ValueDataSource[Int] = 
    self.map(_.findMinMax._2)
        .reduce { (m1,m2) =>
          if(m1 == NODATA) m2
          else if(m2 == NODATA) m1
          else math.max(m1,m2)
         }

  def minMax():ValueDataSource[(Int,Int)] = 
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
