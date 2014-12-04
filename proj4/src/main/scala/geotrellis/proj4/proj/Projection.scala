package geotrellis.proj4.proj

import geotrellis.proj4.ProjCoordinate

trait Projection {

  protected val Eps10 = 1e-10

  protected val RTD = 180 / math.Pi

  protected val DTR = math.Pi / 180

  def project(lplam: Double, lpphi: Double): ProjCoordinate

  def EPSGCode: Int = 0

}
