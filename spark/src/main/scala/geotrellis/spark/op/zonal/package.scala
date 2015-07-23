package geotrellis.spark.op

import geotrellis.spark._
import scala.reflect._
import geotrellis.raster.Tile

package object zonal {
  implicit class ZonalRasterRDDMethodExtensions[K](val rasterRDD: RasterRDD[K, Tile])
    (implicit val keyClassTag: ClassTag[K])
      extends ZonalRasterRDDMethods[K] with Serializable
}
