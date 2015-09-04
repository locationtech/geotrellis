package geotrellis.spark

import geotrellis.proj4.CRS
import geotrellis.raster._
import geotrellis.raster.reproject.{ReprojectOptions, ReprojectView}
import geotrellis.spark.ingest.IngestKey
import org.apache.spark.rdd.RDD

import scala.reflect.ClassTag

package object reproject {
  implicit class ReprojectWrapper[T: IngestKey, TileType: ReprojectView](rdd: RDD[(T, TileType)]) {
    def reproject(destCRS: CRS, options: ReprojectOptions = ReprojectOptions.DEFAULT): RDD[(T, TileType)] =
      Reproject(rdd, destCRS,  options)
  }

  implicit class RasterRDDReprojectWrapper[K: SpatialComponent: ClassTag](rdd: RasterRDD[K]) {
    def reproject(destCRS: CRS, options: ReprojectOptions = ReprojectOptions.DEFAULT): RasterRDD[K] =
      Reproject(rdd, destCRS,  options)
  }

  implicit class MultiBandRasterRDDReprojectWrapper[K: SpatialComponent: ClassTag](rdd: MultiBandRasterRDD[K]) {
    def reproject(destCRS: CRS, options: ReprojectOptions = ReprojectOptions.DEFAULT): MultiBandRasterRDD[K] =
      Reproject(rdd, destCRS,  options)
  }
}
