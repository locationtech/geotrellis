package geotrellis.spark.op

import geotrellis.spark._
import geotrellis.raster.Tile
import reflect.ClassTag

package object elevation {

  implicit class ElevationRasterRDDMethodExtensions[K](val rasterRDD: RasterRDD[K, Tile])
    (implicit val keyClassTag: ClassTag[K], implicit val _sc: SpatialComponent[K])
      extends ElevationRasterRDDMethods[K] with Serializable

}
