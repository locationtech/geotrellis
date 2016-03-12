package geotrellis.spark.tiling

import geotrellis.spark._
import geotrellis.vector._
import geotrellis.util.MethodExtensions

trait TilerKeyMethods[K1, K2] extends MethodExtensions[K1] {
  def extent: Extent
  def translate(spatialKey: SpatialKey): K2
}
