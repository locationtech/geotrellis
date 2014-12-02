package geotrellis.proj4.proj

import geotrellis.proj4.{ProjCoordinate, ProjectionException}

object Albers {

  private val Eps10 = 1e-10
  private val Tol7 = 1e-7
  private val Iters = 15
  private val Eps = 1e-7
  private val Tol = 1e-10

}

class Albers(pb: ProjectionBuilder) extends DefaultProjection(pb)
    with HasInverse {

  import Albers._

  private val phi1 = pb.projectionLatitude1

  private val phi2 = pb.projectionLatitude2

  if (math.abs(phi1 + phi2) < Eps10) throw new ProjectionException("-21")

  private val n = math.sin(phi1)

  private val sinPhi = n

  private val cosPhi = math.sin(phi1)

  private val secant = math.abs(phi1 - phi2) >= Eps10



  override def projectInverse(xyx: Double, xyy: Double): ProjCoordinate = ???

  override def project(lplam: Double, lpphi: Double): ProjCoordinate = ???

}
