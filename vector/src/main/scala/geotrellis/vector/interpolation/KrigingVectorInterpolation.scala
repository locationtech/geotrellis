package geotrellis.vector.interpolation

import geotrellis.vector._
import org.apache.commons.math3.linear.{LUDecomposition, MatrixUtils, RealMatrix}
import org.apache.commons.math3.stat.descriptive.moment.Variance
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

  def getCovariogramMatrix(sv: Function1[Double, Double], points: Seq[PointFeature[Int]]): RealMatrix = {
    //TODO : Select a subdomain(range/cross-validaiton) from the given PointSequence for smaller covariogram computation
    val pointSize = points.size
    val nugget = sv(0)
    val sill: Double = sv(1) - nugget
    val covariogram = Array.ofDim[Double](pointSize, pointSize)
    cfor(0)(_ < pointSize, _ + 1) { row =>
      covariogram(row)(row) = sill - nugget
      cfor(0)(_ < row, _ + 1) { col =>
        covariogram(row)(col) = sill - sv(distance(points(row).geom, points(col).geom))
        covariogram(col)(row) = covariogram(row)(col)
      }
    }

    MatrixUtils.createRealMatrix(covariogram)
  }

  def getSill(sv: Function1[Double, Double], points: Seq[PointFeature[Int]], model: ModelType) : Double = {
    model match {
      case Linear =>
        sv(1) - sv(0)
      case _ =>
        new Variance().evaluate(points.map(x => x.data.toDouble).toArray)

      //Include this sill value in Object of the semivariogram
    }
  }

  def krigingVectorSimple(points: Seq[PointFeature[Int]], radius: Option[Int], chunkSize: Int, lag: Int = 0, model: ModelType): Point => (Double, Double) = {

    pointPredict => {
      val pointSize = points.size
      if (pointSize == 0)
        throw new IllegalArgumentException("No Points in the observation sequence");
      val mean: Double = points.foldLeft(0.0)(_ + _.data) / pointSize
      val sv = Semivariogram(points, radius, lag, model)

      val covariogram: RealMatrix = getCovariogramMatrix(sv, points)
      val covarianceInverse: RealMatrix = new LUDecomposition(covariogram).getSolver.getInverse
      val sill: Double = getSill(sv, points, model)

      val cMatrix: RealMatrix = MatrixUtils.createRealMatrix(Array.tabulate(1, pointSize) { (_, i) => sill - sv(distance(pointPredict, points(i).geom)) })
      val errorMatrix: RealMatrix = MatrixUtils.createRealMatrix(Array.tabulate(pointSize, 1) { (i, _) => points(i).data - mean })
      val prediction: Double = mean + cMatrix.multiply(covarianceInverse).multiply(errorMatrix).getEntry(0, 0)

      val krigingVariance: Double = math.sqrt(sill - cMatrix.multiply(covarianceInverse).multiply(cMatrix.transpose()).getEntry(0, 0))
      (prediction, krigingVariance)
    }
  }

  def krigingVectorOrdinary(points: Seq[PointFeature[Int]], radius: Option[Int], chunkSize: Int, lag: Int = 0, model: ModelType): Point => (Double, Double) = {

    pointPredict => {
      val pointSize = points.size
      if (pointSize == 0)
        throw new IllegalArgumentException("No Points in the observation sequence");
      val sv = Semivariogram(points, radius, lag, model)

      val covariogram: RealMatrix = getCovariogramMatrix(sv, points)
      val covarianceInverse: RealMatrix = new LUDecomposition(covariogram).getSolver.getInverse
      val sill: Double = getSill(sv, points, model)

      val rowOne: RealMatrix = MatrixUtils.createRowRealMatrix(Array.fill(pointSize)(1))
      val dataMatrix: RealMatrix = MatrixUtils.createColumnRealMatrix(points.map(x => x.data.toDouble).toArray)
      val mean_numerator: Double = rowOne.multiply(covarianceInverse).multiply(dataMatrix).getEntry(0, 0)
      val mean_denominator: Double = rowOne.multiply(covarianceInverse).multiply(dataMatrix.transpose()).getEntry(0, 0)
      val mean: Double = mean_numerator / mean_denominator

      val cMatrix: RealMatrix = MatrixUtils.createRealMatrix(Array.tabulate(1, pointSize) { (_, i) => sill - sv(distance(pointPredict, points(i).geom)) })
      val errorMatrix: RealMatrix = MatrixUtils.createRealMatrix(Array.tabulate(pointSize, 1) { (i, _) => points(i).data - mean })
      val prediction: Double = mean + cMatrix.multiply(covarianceInverse).multiply(errorMatrix).getEntry(0, 0)

      val krigingVariance: Double = math.sqrt(sill - cMatrix.multiply(covarianceInverse).multiply(cMatrix.transpose()).getEntry(0, 0))
      (prediction, krigingVariance)
    }
  }

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