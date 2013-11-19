package geotrellis.raster.op.local

import geotrellis._

import geotrellis.data._

object ColorRaster {
  def apply(r:Op[Raster],
            breaksToColors:Op[Map[Int,Int]]):Op[Raster] = 
    (r,breaksToColors).map { (r,breaksToColors) => 
      IntColorMap(breaksToColors).render(r)
    }.withName("IntColorMap")

  def apply(r:Op[Raster],
            breaksToColors:Op[Map[Int,Int]],
            options:ColorMapOptions):Op[Raster] = 
    (r,breaksToColors).map { (r,breaksToColors) => 
      IntColorMap(breaksToColors,options).render(r)
    }.withName("IntColorMap")


  def apply(r:Op[Raster],
            breaksToColors:Op[Map[Double,Int]])(implicit d:DI):Op[Raster] =
    (r,breaksToColors).map { (r,breaksToColors) => 
      DoubleColorMap(breaksToColors).render(r)
    }.withName("IntColorMap")

  def apply(r:Op[Raster],
            breaksToColors:Op[Map[Double,Int]],
            options:ColorMapOptions)(implicit d:DI):Op[Raster] = 
    (r,breaksToColors).map { (r,breaksToColors) => 
      DoubleColorMap(breaksToColors,options).render(r)
    }.withName("IntColorMap")
}
