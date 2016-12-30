package geotrellis.pointcloud

import geotrellis.spark.SpatialKey
import geotrellis.spark.tiling.TilerKeyMethods
import geotrellis.util._


package object spark extends dem.Implicits with tiling.Implicits {
  implicit class withProjectedExtent3DTilerKeyMethods[K: Component[?, ProjectedExtent3D]](val self: K) extends TilerKeyMethods[K, SpatialKey] {
    def extent = self.getComponent[ProjectedExtent3D].extent3d.toExtent
    def translate(spatialKey: SpatialKey) = spatialKey
  }
}

