package geotrellis.proj4.proj

import geotrellis.proj4.ProjCoordinate

class DefaultProjection(pb: ProjectionBuilder) extends Projection {

  protected val a = pb.ellipsoid.equatorRadius

  protected val e = pb.ellipsoid.eccentricity

  protected val es = pb.ellipsoid.eccentricity2

  protected val spherical = e == 0

  protected val one_es = 1 - es

  protected val rone_es = 1 / one_es

  protected val totalScale = a * pb.fromMetres

  protected val totalFalseEasting = pb.falseEasting * pb.fromMetres

  protected val totalFalseNorthing = pb.falseNorthing * pb.fromMetres

  def project(lplam: Double, lpphi: Double): ProjCoordinate = ???

}
