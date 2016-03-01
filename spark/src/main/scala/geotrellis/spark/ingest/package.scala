package geotrellis.spark

import geotrellis.proj4.CRS
import geotrellis.raster.resample.{NearestNeighbor, ResampleMethod}
import geotrellis.spark.tiling.{ TilerKeyMethods, LayoutDefinition, LayoutScheme }
import geotrellis.vector._
import geotrellis.raster._
import org.apache.spark.rdd._
import monocle.syntax._


package object ingest {
  type ProjectedExtentComponent[T] = KeyComponent[T, ProjectedExtent]

  implicit class ProjectedExtentComponentMethods[T: ProjectedExtentComponent](key: T) {
    val _projectedExtent = implicitly[ProjectedExtentComponent[T]]

    def projectedExtent: ProjectedExtent = _projectedExtent.lens.get(key)

    def updateProjectedExtent(pe: ProjectedExtent): T =
      _projectedExtent.lens.set(pe)(key)
  }

  implicit object ProjectedExtentComponent extends IdentityComponent[ProjectedExtent]

  implicit class withCollectMetadataMethods[
    K1: (? => TilerKeyMethods[K1, K2]),
    V <: CellGrid,
    K2: SpatialComponent: Boundable
  ](rdd: RDD[(K1, V)]) extends Serializable {
    def collectMetaData(crs: CRS, layoutScheme: LayoutScheme): (Int, RasterMetaData[K2]) = {
      RasterMetaData.fromRdd(rdd, crs, layoutScheme)
    }

    def collectMetaData(crs: CRS, layout: LayoutDefinition): RasterMetaData[K2] = {
      RasterMetaData.fromRdd(rdd, crs, layout)
    }
  }
}
