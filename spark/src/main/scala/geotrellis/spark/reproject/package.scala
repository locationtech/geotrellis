package geotrellis.spark

import geotrellis.proj4.CRS
import geotrellis.raster._
import geotrellis.raster.reproject._
import geotrellis.spark.ingest.IngestKey
import geotrellis.spark.tiling._
import org.apache.spark.rdd.RDD

import scala.reflect.ClassTag

package object reproject {
  implicit class ReprojectWrapper[T: IngestKey, TileType: ReprojectView](val rdd: RDD[(T, TileType)]) {
    def reproject(destCRS: CRS): Reproject.Apply[RDD[(T, TileType)]] =
      IngestKeyReproject(rdd, destCRS)
  }

  implicit class RasterRDDReprojectWrapper[K: SpatialComponent: ClassTag](rdd: RasterRDD[K]) {
    def reproject(destCRS: CRS): TileReproject.Apply[K] =
      TileReproject(rdd, destCRS)
  }

  implicit class MultiBandRasterRDDReprojectWrapper[K: SpatialComponent: ClassTag](rdd: MultiBandRasterRDD[K]) {
    def reproject(destCRS: CRS, layoutScheme: LayoutScheme): MultiBandTileReproject.Apply[K] =
      MultiBandTileReproject(rdd, destCRS, layoutScheme)
  }
}
