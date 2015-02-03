package geotrellis.spark

import geotrellis.vector._
import geotrellis.raster._
import geotrellis.raster.reproject._
import geotrellis.proj4.CRS
import org.apache.spark.rdd._
import monocle.syntax._
import geotrellis.spark._
import spire.syntax.cfor._

package object ingest {
  type Tiler[T, K] = (RDD[(T, Tile)], RasterMetaData) => RasterRDD[K]
  type IngestKey[T] = KeyComponent[T, ProjectedExtent]

  implicit class IngestKeyWrapper[T: IngestKey](key: T) {
    val _projectedExtent = implicitly[IngestKey[T]]

    def projectedExtent: ProjectedExtent = key &|-> _projectedExtent.lens get

    def updateProjectedExtent(pe: ProjectedExtent): T =
      key &|-> _projectedExtent.lens set(pe)
  }

  implicit object ProjectedExtentComponent extends IdentityComponent[ProjectedExtent]

  implicit def projectedExtentToSpatialKeyTiler: Tiler[ProjectedExtent, SpatialKey] = {
      val getExtent = (inKey: ProjectedExtent) => inKey.extent
      val createKey = (inKey: ProjectedExtent, spatialComponent: SpatialKey) => spatialComponent
      Tiler(getExtent, createKey)
    }

  implicit class ReprojectWrapper[T: IngestKey](rdd: RDD[(T, Tile)]) {
    def reproject(destCRS: CRS): RDD[(T, Tile)] = Reproject(rdd, destCRS)
  }
}
