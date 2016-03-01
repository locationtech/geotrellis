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

object ProjectedExtentComponentReproject {
  import geotrellis.raster.reproject.Reproject.Options


  /** Reproject the given RDD and modify the key with the new CRS and extent
    */
  def apply[K: ProjectedExtentComponent, V <: CellGrid: (? => TileReprojectMethods[V])](
    rdd: RDD[(K, V)],
    destCrs: CRS,
    options: Options
  ): RDD[(K, V)] =
    rdd.map { case (key, tile) =>
      val ProjectedExtent(extent, crs) = key.projectedExtent
      val Raster(newTile , newExtent) =
        tile.reproject(extent, crs, destCrs, options)
      val newKey = key.updateProjectedExtent(ProjectedExtent(newExtent, destCrs))
      (newKey, newTile)
    }
}
