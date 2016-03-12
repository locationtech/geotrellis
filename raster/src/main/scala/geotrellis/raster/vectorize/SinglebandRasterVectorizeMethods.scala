package geotrellis.raster.vectorize

import geotrellis.raster._
import geotrellis.vector._
import geotrellis.util.MethodExtensions

trait SinglebandRasterVectorizeMethods extends MethodExtensions[Raster[Tile]] {
  def toVector(regionConnectivity: Connectivity = FourNeighbors): List[PolygonFeature[Int]] =
    Vectorize(self.tile, self.extent, regionConnectivity)
}
