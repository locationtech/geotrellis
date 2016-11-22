package geotrellis.spark.io

import geotrellis.spark._
import geotrellis.spark.pdal.ProjectedExtent3D
import geotrellis.spark.tiling.TilerKeyMethods
import geotrellis.util._

package object pdal {
  implicit class withProjectedExtent3DTilerKeyMethods[K: Component[?, ProjectedExtent3D]](val self: K) extends TilerKeyMethods[K, SpatialKey] {
    def extent = self.getComponent[ProjectedExtent3D].extent3d.toExtent
    def translate(spatialKey: SpatialKey) = spatialKey
  }
}
