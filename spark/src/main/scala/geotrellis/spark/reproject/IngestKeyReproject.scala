package geotrellis.spark.reproject

import geotrellis.proj4._
import geotrellis.raster._
import geotrellis.raster.reproject._
import geotrellis.raster.resample._
import geotrellis.spark._
import geotrellis.spark.ingest._
import geotrellis.vector._
import org.apache.spark.rdd._

import scala.reflect.ClassTag

object IngestKeyReproject {
  def apply[T: IngestKey, TileType: ReprojectView](
      rdd: RDD[(T, TileType)],
      destCRS: CRS): Reproject.Apply[RDD[(T, TileType)]] = 
    new Reproject.Apply[RDD[(T, TileType)]] {
      def apply(method: ResampleMethod = NearestNeighbor, errorThreshold: Double = 0.125): RDD[(T, TileType)] =
        rdd.map { case (key, tile) =>
          val ProjectedExtent(extent, crs) = key.projectedExtent
          val Product2(newTile , newExtent) = tile.reproject(extent, crs, destCRS)(method = method, errorThreshold = errorThreshold)
          val newKey = key.updateProjectedExtent(ProjectedExtent(newExtent, destCRS))
          (newKey, newTile)
        }
    }
}
