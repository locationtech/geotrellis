package geotrellis.proj4.proj

import geotrellis.proj4.ProjCoordinate

object AugustProjection extends Projection {

  lazy val M = 1 + 1.0 / 3

  override def project(lplam: Double, lpphi: Double) = {
    val t = math.tan(0.5 * lpphi)
    val c1 = math.sqrt(1 - t * t)
    val c = 1 + c1 * math.cos(lplam * 0.5)
    val x1 = math.sin(lplam * 0.5) * c1 / c
    val y1 = t / c

    val x = M * x1 * (3 + x1 * x1 - 3 * y1 * y1)
    val y = M * y1 * (3 + 3 * x1 * x1 - y1 * y1)

    ProjCoordinate(x, y)
  }

  override def toString() = "August Epicycloidal"

}
