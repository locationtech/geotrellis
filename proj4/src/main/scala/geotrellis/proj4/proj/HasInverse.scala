package geotrellis.proj4.proj

import geotrellis.proj4.ProjCoordinate

trait HasInverse {

  def projectInverse(xyx: Double, xyy: Double): ProjCoordinate

}
