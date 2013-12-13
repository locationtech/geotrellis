package geotrellis.raster.op.local

import geotrellis._

import scala.annotation.tailrec

class RasterReducer(handle:(Int,Int)=>Int)(handleDouble:(Double,Double)=>Double) {
  // This class benchmarks fast, if you change it be sure to compare performance.
  def apply(seq:Seq[Raster]):Raster =
    handleRasters(seq.toList)

  @tailrec final def reduce(d:Raster, rasters:List[Raster]):Raster = {
    rasters match {
      case Nil => d
      case r :: rs => if (r.isFloat) {
        reduceDouble(d.combineDouble(r)(handleDouble), rs)
      } else {
        reduce(d.combine(r)(handle), rs)
      }
    }
  }

  @tailrec final def reduceDouble(d:Raster, rasters:List[Raster]):Raster = {
    rasters match {
      case Nil => d
      case r :: rs => reduceDouble(d.combineDouble(r)(handleDouble), rs)
    }
  }

  def handleRasters(rasters:List[Raster]) = {
    val (r :: rs) = rasters

    if (r.isFloat) {
      reduceDouble(r, rs)
    } else {
      reduce(r, rs)
    }
  }
}
