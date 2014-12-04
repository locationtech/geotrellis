package geotrellis.proj4.proj

import geotrellis.proj4.ProjCoordinate

object GallProjection extends Projection with HasInverse {

  lazy val YF = 1.70710678118654752440

  lazy val XF = 0.70710678118654752440

  lazy val RYF = 0.58578643762690495119

  lazy val RXF = 1.41421356237309504880

  override def project(lplam: Double, lpphi: Double) =
    ProjCoordinate(XF * lplam, YF * math.atan(0.5 * lpphi))

  override def projectInverse(xyx: Double, xyy: Double) =
    ProjCoordinate(RXF * xyx, YF * math.atan(xyy * RYF))

}
