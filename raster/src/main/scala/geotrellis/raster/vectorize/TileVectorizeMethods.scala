package geotrellis.raster.vectorize

import geotrellis.raster._
import geotrellis.vector._
import geotrellis.util.MethodExtensions

trait VectorizeMethods extends MethodExtensions[Tile] {
  def toVector(extent: Extent, regionConnectivity: Connectivity = FourNeighbors): List[PolygonFeature[Int]] =
    Vectorize(self, extent, regionConnectivity)
}
