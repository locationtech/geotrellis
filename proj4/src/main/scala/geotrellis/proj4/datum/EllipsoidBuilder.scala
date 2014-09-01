package geotrellis.proj4.datum

class EllipsoidBuilder() {
  private final val SIXTH = 0.1666666666666666667
  private final val RA4 = 0.04722222222222222222
  private final val RA6 = 0.02215608465608465608
  private final val RV4 = 0.06944444444444444444
  private final val RV6 = 0.04243827160493827160

  private var a = Double.NaN
  private var es = Double.NaN

  def setA(value: Double): EllipsoidBuilder =
    { a = value ; this }

  def setB(b: Double): EllipsoidBuilder =
    { es = 1 - (b * b) / (a * a) ; this }

  def setES(value: Double): EllipsoidBuilder =
    { es = value ; this }

  def setRF(rf: Double): EllipsoidBuilder =
    { es = rf * (2 - rf) ; this }

  def setRA(): EllipsoidBuilder =
    { a = a * (1 - es * (SIXTH + es * (RA4 + es * RA6))) ; this }

  def setF(f: Double) =
    { es = (1 / f) * (2 - (1 / f)) ; this }

  def build(): Ellipsoid =
    if(java.lang.Double.isNaN(a) || java.lang.Double.isNaN(es))
      Ellipsoid.NULL
    else
      Ellipsoid("user", a, es, "User-defined")
}
