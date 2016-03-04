package geotrellis.spark

import geotrellis.proj4.CRS
import geotrellis.raster.resample.{NearestNeighbor, ResampleMethod}
import geotrellis.spark.tiling.{ TilerKeyMethods, LayoutDefinition, LayoutScheme }
import geotrellis.vector._
import geotrellis.raster._
import org.apache.spark.rdd._
import monocle.syntax._


package object ingest {
  implicit class withCollectMetadataMethods[K1, V <: CellGrid](rdd: RDD[(K1, V)]) extends Serializable {
    def collectMetaData[K2: Boundable: SpatialComponent](crs: CRS, layoutScheme: LayoutScheme)
        (implicit ev: K1 => TilerKeyMethods[K1, K2]): (Int, RasterMetaData[K2]) = {
      RasterMetaData.fromRdd(rdd, crs, layoutScheme)
    }

    def collectMetaData[K2: Boundable: SpatialComponent](crs: CRS, layout: LayoutDefinition)
        (implicit ev: K1 => TilerKeyMethods[K1, K2]): RasterMetaData[K2] = {
      RasterMetaData.fromRdd(rdd, crs, layout)
    }
  }
}
