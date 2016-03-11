package geotrellis.spark.mask

import geotrellis.raster._
import geotrellis.raster.rasterize.Rasterize.Options
import geotrellis.spark._
import geotrellis.spark.TileLayerRDD
import geotrellis.vector._
import scala.reflect.ClassTag


object Implicits extends Implicits

trait Implicits {
  implicit class withRDDMaskMethods[K: SpatialComponent: ClassTag](val self: TileLayerRDD[K])
      extends TileLayerRDDMaskMethods[K]
}
