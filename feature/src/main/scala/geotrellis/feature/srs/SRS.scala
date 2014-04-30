package geotrellis.feature.srs

trait SRS {
  val name: String

  def toEllipsoidal(x: Double, y: Double): (Double, Double)
  def toCartesian(x: Double, y: Double): (Double, Double)

  def toEllipsoidal(t: (Double, Double)): (Double, Double) = toEllipsoidal(t._1, t._2)
  def toCartesian(t: (Double, Double)): (Double, Double) =  toCartesian(t._1, t._2)
}
