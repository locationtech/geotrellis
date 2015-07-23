package geotrellis.spark

import scala.reflect.ClassTag
import geotrellis.raster.Tile

trait RasterRDDMethods[K] { 
  implicit val keyClassTag: ClassTag[K]

  val rasterRDD: RasterRDD[K, Tile]  
}
