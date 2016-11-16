package geotrellis.raster.interpolation

import geotrellis.raster._
import geotrellis.vector._
import geotrellis.util.MethodExtensions

abstract class InverseDistanceWeightedMethods[D](implicit ev: D => Double) extends MethodExtensions[Traversable[PointFeature[D]]] {
  def inverseDistanceWeighted(rasterExtent: RasterExtent, options: InverseDistanceWeighted.Options): Raster[Tile] =
    InverseDistanceWeighted(self, rasterExtent, options)

  def inverseDistanceWeighted(rasterExtent: RasterExtent): Raster[Tile] =
    inverseDistanceWeighted(rasterExtent, InverseDistanceWeighted.Options.DEFAULT)
}
