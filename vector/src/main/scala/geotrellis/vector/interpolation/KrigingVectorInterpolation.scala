package geotrellis.vector.interpolation

import geotrellis.vector._
import org.apache.commons.math3.linear.{LUDecomposition, MatrixUtils, RealMatrix}
import spire.syntax.cfor._

abstract sealed class KrigingVectorInterpolationMethod

case object KrigingVectorSimple extends KrigingVectorInterpolationMethod
case object KrigingVectorOrdinary extends KrigingVectorInterpolationMethod
case object KrigingVectorUniversal extends KrigingVectorInterpolationMethod
case object KrigingVectorGeo extends KrigingVectorInterpolationMethod

object KrigingVectorInterpolationMethod {
  val DEFAULT = KrigingVectorSimple
}

object KrigingVectorInterpolation {

  def distance(p1: Point, p2: Point) = math.abs(math.sqrt(math.pow(p1.x - p2.x, 2) + math.pow(p1.y - p2.y, 2)))

  def getCovariogramMatrix(sv: Function1[Double, Double], points: Seq[PointFeature[Int]]): Array[Array[Double]] = {
    val pointSize = points.size
    val sill: Double = sv(1) - sv(0)
    val covariogram = Array.tabulate(pointSize, pointSize) { (i, j) => sill - sv(distance(points(i).geom, points(j).geom)) }

    covariogram
  }

  def krigingVectorSimple(points: Seq[PointFeature[Int]], radius: Option[Int], chunkSize: Int, lag: Int = 0, model: ModelType): Point => (Double, Double) = {

    pointPredict => {
      val pointSize = points.size
      if (pointSize == 0)
        throw new IllegalArgumentException("No Points in the observation sequence");
      model match {
        case Linear => {
          val mean: Double = (points.foldLeft(0.0)(_ + _.data)) / pointSize
          val sv = Semivariogram(points, radius, lag, model)

          val covariogram: RealMatrix = MatrixUtils.createRealMatrix(getCovariogramMatrix(sv, points))
          val covarianceInverse: RealMatrix = new LUDecomposition(covariogram).getSolver().getInverse()

          val nugget: Double = sv(0)
          val sill: Double = sv(1) - nugget

          val cMatrix: RealMatrix = MatrixUtils.createRealMatrix(Array.tabulate(1, pointSize) { (_, i) => sill - sv(distance(pointPredict, points(i).geom)) })
          val errorMatrix: RealMatrix = MatrixUtils.createRealMatrix(Array.tabulate(pointSize, 1) { (i, _) => points(i).data - mean })
          val prediction: Double = mean + (cMatrix.multiply(covarianceInverse)).multiply(errorMatrix).getEntry(0, 0)

          val krigingVariance: Double = math.sqrt(sill - (cMatrix.multiply(covarianceInverse)).multiply(cMatrix.transpose()).getEntry(0, 0))
          (prediction, krigingVariance)
        }
        case _ => ???
      }
    }
  }

  def krigingVectorOrdinary(points: Seq[PointFeature[Int]], radius: Option[Int], chunkSize: Int, lag: Int = 0, model: ModelType): Point => (Double, Double) = ???
  def krigingVectorUniversal(points: Seq[PointFeature[Int]], radius: Option[Int], chunkSize: Int, lag: Int = 0, model: ModelType): Point => (Double, Double) = ???
  def krigingVectorGeo(points: Seq[PointFeature[Int]], radius: Option[Int], chunkSize: Int, lag: Int = 0, model: ModelType): Point => (Double, Double) = ???

  def apply(method: KrigingVectorInterpolationMethod, points: Seq[PointFeature[Int]], radius: Option[Int], chunkSize: Int, lag: Int = 0, model: ModelType): Point => (Double, Double) = {
    method match {
      case KrigingVectorSimple => krigingVectorSimple(points, radius, chunkSize, lag, model)
      case KrigingVectorOrdinary => krigingVectorOrdinary(points, radius, chunkSize, lag, model)
      case KrigingVectorUniversal => krigingVectorUniversal(points, radius, chunkSize, lag, model)
      case KrigingVectorGeo => krigingVectorGeo(points, radius, chunkSize, lag, model)
      case _ => {
        println("The Kriging type is not recognized, using default Kriging type")
        apply(KrigingVectorInterpolationMethod.DEFAULT, points, radius, chunkSize, lag, model)
      }
    }
  }
}