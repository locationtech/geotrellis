package geotrellis.spark

import geotrellis.proj4.CRS
import geotrellis.raster.resample.{NearestNeighbor, ResampleMethod}
import geotrellis.spark.tiling.{LayoutDefinition, LayoutScheme}
import geotrellis.vector._
import geotrellis.raster._
import org.apache.spark.rdd._
import monocle.syntax._


package object ingest {
  type IngestKey[T] = KeyComponent[T, ProjectedExtent]

  implicit class ProjectedExtentComponentMethods[T: IngestKey](key: T) {
    val _projectedExtent = implicitly[IngestKey[T]]

    def projectedExtent: ProjectedExtent = key &|-> _projectedExtent.lens get

    def updateProjectedExtent(pe: ProjectedExtent): T =
      key &|-> _projectedExtent.lens set (pe)
  }

  implicit object ProjectedExtentComponent extends IdentityComponent[ProjectedExtent]

  implicit class withCollectMetadataMethods[K: IngestKey, V <: CellGrid](rdd: RDD[(K, V)]) extends Serializable {
    def collectMetaData(crs: CRS, layoutScheme: LayoutScheme): (Int, RasterMetaData) = {
      RasterMetaData.fromRdd(rdd, crs, layoutScheme)(_.projectedExtent.extent)
    }

    def collectMetaData(crs: CRS, layout: LayoutDefinition): RasterMetaData = {
      RasterMetaData.fromRdd(rdd, crs, layout)(_.projectedExtent.extent)
    }
  }
}
