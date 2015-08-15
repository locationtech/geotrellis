package geotrellis.spark.op.local

import geotrellis.spark._

import scala.reflect.ClassTag

package object spatial {

  implicit class LocalSpatialRasterRDDExtensions[K: SpatialComponent](val rasterRDD: RasterRDD[K])(
    implicit val keyClassTag: ClassTag[K]) extends LocalSpatialRasterRDDMethods[K] { }

}
