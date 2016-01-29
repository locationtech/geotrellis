package geotrellis.raster.reproject

import geotrellis.raster._

object Implicits extends Implicits

trait Implicits {
  implicit class withProjectedRasterReprojectMethods[T <: CellGrid](self: ProjectedRaster[T])(implicit ev: Raster[T] => RasterReprojectMethods[Raster[T]])
    extends ProjectedRasterReprojectMethods[T](self)
}
