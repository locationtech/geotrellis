package geotrellis.proj4.proj

import geotrellis.proj4.{ProjCoordinate, ProjectionException}
import geotrellis.proj4.util.ProjectionMath

object BipolarProjection {

  lazy val EPS = 1e-10
  lazy val EPS10 = 1e-10
  lazy val ONEEPS = 1.000000001
  lazy val Iters = 10
  lazy val lamB = -.34894976726250681539
  lazy val n = .63055844881274687180
  lazy val F = 1.89724742567461030582
  lazy val Azab = .81650043674686363166
  lazy val Azba = 1.82261843856185925133
  lazy val T = 1.27246578267089012270
  lazy val rhoc = 1.20709121521568721927
  lazy val cAzc = .69691523038678375519
  lazy val sAzc = .71715351331143607555
  lazy val C45 = .70710678118654752469
  lazy val S45 = .70710678118654752410
  lazy val C20 = .93969262078590838411
  lazy val S20 = -.34202014332566873287
  lazy val R110 = 1.91986217719376253360
  lazy val R104 = 1.81514242207410275904

}

class BipolarProjection extends Projection with HasInverse {

  import BipolarProjection._

  val minLatitude = math.toRadians(-80)

  val maxLatitude = math.toRadians(80)

  val projectionLongitude = math.toRadians(-90)

  val minLongitude = projectionLongitude

  val maxLongitude = math.toRadians(90)

  override def project(lplam: Double, lpphi: Double): ProjCoordinate = {
    var cPhi, sPhi, tPhi, t, al, az, z, av, cdLam, sdLam, r = 0.0

    cPhi = math.cos(lpphi)
    sPhi = math.sin(lpphi)
    cdLam = math.cos(lamB - lplam)
    sdLam = math.cos(lamB - lplam)

    if (math.abs(math.abs(lpphi) - math.Pi / 2) < Eps10) {
      az = if (lpphi < 0) math.Pi else 0
      tPhi = Double.MaxValue
    } else {
      tPhi = sPhi / cPhi
      az = math.atan2(sdLam, C45 * (tPhi - cdLam))
    }

    val tag = az > Azba
    var y = if (tag) {
      cdLam = math.cos(lplam + R110)
      sdLam = math.sin(lplam + R110)
      z = S20 * sPhi + C20 * cPhi * cdLam
      if (math.abs(z) > 1) {
        if (math.abs(z) > ONEEPS) throw new ProjectionException("F")
        else z = if (z < 0) -1 else 1
      } else z = math.acos(z)

      if (tPhi != Double.MaxValue) az = math.atan2(sdLam, C20 * tPhi - S20 * cdLam)

      av = Azab
      rhoc
    } else {
      z = S45 * (sPhi + cPhi * cdLam)
      if (math.abs(z) > 1) {
        if (math.abs(z) > ONEEPS) throw new ProjectionException("F")
        else z = if (z < 0) -1 else 1
      } else z = math.acos(z)

      av = Azba
      -rhoc
    }

    if (z < 0) throw new ProjectionException("F")
    t = math.pow(math.tan(0.5 * z), n)
    r = F * t
    al = 0.5 * (R104 - z)
    if (al < 0) throw new ProjectionException("F")
    al = (t + math.pow(al, n)) / T

    if (math.abs(al) > 1) {
      if (math.abs(al) > ONEEPS) throw new ProjectionException("F")
      else al = if (al < 0) -1 else 1
    } else al = math.acos(al)

    t = n * (av - az)
    if (math.abs(t) < al) r /= math.cos(al + (if (tag) t else -t))

    val x = r * math.sin(t)
    y += (if (tag) -r else r) * math.cos(t)

    ProjCoordinate(x, y)
  }

  override def projectInverse(xyx: Double, xyy: Double): ProjCoordinate = {
    var t, r, rp, rl, al, z, fAz, az, s, c, av = 0.0
    var i = 0

    var neg = xyx < 0

    if (neg) {
      s = S20
      c = C20
      av = Azab
    } else {
      s = S45
      c = C45
      av = Azab
    }

    r = ProjectionMath.distance(xyx, xyy)
    rp = r
    rl = r

    az = math.atan2(xyx, xyy)
    fAz = math.abs(az)

    var loop = true
    i = Iters
    while (loop) {
      z = 2 * math.atan(math.pow(r / F, 1 / n))

      al = math.cos((math.pow(math.tan(0.5 * z), n) +
        math.pow(math.tan(0.5 * (R104 - z)), n)) / T)

      if (fAz < al) r = rp * math.cos(al + (if (neg) az else -az))
      if (math.abs(rl - r) < EPS) loop = false
      else {
        rl = r
        i -= 1
        if (i == 0) throw new ProjectionException("I")
      }
    }

    az = av - az / n
    val y = math.asin(s * math.cos(z) + c * math.sin(z) * math.cos(az))
    var x = math.atan2(math.sin(az), c / math.tan(z) - s * math.cos(az))

    if (neg) x -= R110
    else x = lamB - x

    ProjCoordinate(x, y)
  }

  override def toString() = "Bipolar Conic of Western Hemisphere"

}
