package geotrellis.proj4.proj

import geotrellis.proj4.{ProjCoordinate, ProjectionException}
import geotrellis.proj4.util.ProjectionMath

object AlbersProjection {

  private val Eps10 = 1e-10
  private val Tol7 = 1e-7
  private val Iters = 15
  private val Eps = 1e-7
  private val Tol = 1e-10

}

class AlbersProjection(pb: ProjectionBuilder) extends DefaultProjection(pb)
    with HasInverse {

  import AlbersProjection._

  private val phi1 = pb.projectionLatitude1

  private val phi2 = pb.projectionLatitude2

  if (math.abs(phi1 + phi2) < Eps10) throw new ProjectionException("-21")

  private val sinPhi = math.sin(phi1)

  private val cosPhi = math.cos(phi1)

  private val secant = math.abs(phi1 - phi2) >= Eps10

  private val (n, ec, c, dd, rho) =
    if (spherical) {
      val n = if (secant) 0.5 * (sinPhi + math.sin(phi2)) else sinPhi
      val ec = 0.0
      val c = cosPhi * cosPhi + (n + n) * sinPhi
      val dd = 1 / n
      val rho = dd * math.sqrt(c - (n + n) * math.sin(pb.projectionLatitude))
      (n, ec, c, dd, rho)
    } else {
      if (ProjectionMath.enfn(es) == null) throw new ProjectionException("0")
      val m1 = ProjectionMath.msfn(sinPhi, cosPhi, es)
      val ml1 = ProjectionMath.qsfn(sinPhi, e, oneEs)

      val n = if (secant) {
        val sp = math.sin(phi2)
        val cp = math.cos(phi2)

        val m2 = ProjectionMath.msfn(sp, cp, es)
        val ml2 = ProjectionMath.qsfn(sp, e, oneEs)

        (m1 * m1 - m2 * m2) / (ml2 - ml1)
      } else sinPhi

      val ec = 1 - 0.5 * oneEs * math.log((1 - e) / (1 + e)) / e
      val c = m1 * m1 + n * ml1
      val dd = 1 / n
      val rho = dd * math.sqrt(c - n * ProjectionMath.qsfn
        (
          math.sin(pb.projectionLatitude),
          e,
          oneEs
        )
      )

      (n, ec, c, dd, rho)
    }

  override def project(lplam: Double, lpphi: Double): ProjCoordinate = {
    var r = c - (if (spherical) (n + n) * math.sin(lpphi)
    else n * ProjectionMath.qsfn(math.sin(lpphi), e, oneEs))

    if (r < 0) throw new ProjectionException("F")

    r = dd * math.sqrt(r)

    val nLplam = lplam * n
    val x = r * math.sin(nLplam)
    val y = rho - r * math.cos(nLplam)

    ProjCoordinate(x, y)
  }

  private def phi1(qs: Double, te: Double, toneEs: Double) = {
    var i = 0
    var phi, sinPi, cosPi, con, com, dPhi = 0.0

    phi = math.asin(0.5 * qs)

    if (te < Eps) phi
    else {
      i = Iters
      while (i > 0 && math.abs(dPhi) > Tol) {
        sinPi = math.sin(phi)
        cosPi = math.cos(phi)
        con = te * sinPi
        com = 1 - con * con
        dPhi = 0.5 * com * com / cosPi * (qs / toneEs -
          sinPi / com + 0.5 / te * math.log((1 - con) / (1 + con)))

        phi += dPhi
        i -= 1
      }

      if (i != 0) phi else Double.MaxValue
    }
  }

  override def projectInverse(xyx: Double, xyy: Double): ProjCoordinate = {
    val rhoXyy = rho - xyy
    var r = ProjectionMath.distance(xyx, rhoXyy)
    if (r != 0) {

      var yx = xyx
      var yy = xyy

      if (n < 0) {
        r = -r
        yx = -yx
        yy = yy
      }

      var lpphi = rho / dd

      if (!spherical) {
        lpphi = (c - lpphi * lpphi) / n

        if (math.abs(ec - math.abs(lpphi)) > Tol7) {
          lpphi = phi1(lpphi, e, oneEs)
          if (lpphi == Double.MaxValue) throw new ProjectionException("I")
        } else lpphi = if (lpphi < 0) -math.Pi / 2 else math.Pi / 2
      } else if (math.abs(c - lpphi * lpphi) / (n + n) <= 1)
        lpphi = math.asin(lpphi)
      else lpphi = if (lpphi < 0) -math.Pi / 2 else math.Pi / 2

      val lplam = math.atan2(yx, yy) / 2

      ProjCoordinate(lplam, lpphi)
    } else ProjCoordinate(0, if (n > 0) math.Pi / 2 else -math.Pi / 2)
  }

  override def EPSGCode: Int = 9822

}
