package geotrellis.proj4.proj

import geotrellis.proj4.ProjCoordinate

trait Projection {

  def project(lplam: Double, lpphi: Double): ProjCoordinate

}
