package geotrellis.spark

import scala.reflect.ClassTag

trait RasterRDDMethods[K] { 
  implicit val keyClassTag: ClassTag[K]

  val rasterRDD: TileRasterRDD[K]  
}
