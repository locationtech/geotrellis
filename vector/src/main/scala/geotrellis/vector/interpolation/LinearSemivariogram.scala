package geotrellis.vector.interpolation

import geotrellis.vector._
import org.apache.commons.math3.stat.regression.SimpleRegression

object LinearSemivariogram {
  def apply(sill: Double, nugget: Double): Double => Double =
    { x: Double => x * sill + nugget }

  //def apply(pts: Seq[PointFeature[Double]], radius: Option[Double] = None, lag: Double = 0, model: ModelType): Semivariogram = {
  def apply(pts: Seq[PointFeature[Double]], radius: Option[Double] = None, lag: Double = 0): Double => Double = {
    // Construct slope and intercept
    val regression = new SimpleRegression
    val empiricalSemivariogram: Seq[(Double, Double)] = EmpiricalVariogram.linear(pts, radius, lag)
    for((x, y) <- empiricalSemivariogram) { regression.addData(x, y) }
    val slope = regression.getSlope
    val intercept = regression.getIntercept
    //Semivariogram({ x => slope * x + intercept }, 0, slope, intercept)
    //Semivariogram(0, slope, intercept)
    x => slope * x + intercept
  }
}