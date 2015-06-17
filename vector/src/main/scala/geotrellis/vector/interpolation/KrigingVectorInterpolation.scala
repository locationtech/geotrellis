package geotrellis.vector.interpolation

import geotrellis.vector._
import org.apache.commons.math3.linear.{LUDecomposition, MatrixUtils, RealMatrix, CholeskyDecomposition}
import org.apache.commons.math3.stat.descriptive.moment.Variance
import spire.syntax.cfor._

trait KrigingInterpolationMethod{
  def createPredictor(): Point => (Double, Double)

  def distance(p1: Point, p2: Point) = math.abs(math.sqrt(math.pow(p1.x - p2.x, 2) + math.pow(p1.y - p2.y, 2)))

  def getCovariogramMatrix(sv: Function1[Double, Double], points: Seq[PointFeature[Int]]): RealMatrix = {
    //TODO : Select a subdomain(range/cross-validaiton) from the given PointSequence for smaller covariogram computation
    //Range selection is done, execute the cross-validation as well; added features for bandwidth input
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

  def getPredictionSet(points: Seq[PointFeature[Int]], bandwidth: Double, point: Point): Seq[PointFeature[Int]] = {
    points.filter(x => distance(x.geom, point) < bandwidth)
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
}

class KrigingSimple(points: Seq[PointFeature[Int]], radius: Option[Int], chunkSize: Int, lag: Int = 0, model: ModelType) extends KrigingInterpolationMethod {
  def createPredictor(): Point => (Double, Double) = {
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
}

class KrigingOrdinary(points: Seq[PointFeature[Int]], radius: Option[Int], chunkSize: Int, lag: Int = 0, model: ModelType) extends KrigingInterpolationMethod {
  def createPredictor(): Point => (Double, Double) = {
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
}

/*
class KrigingUniversal(points: Seq[PointFeature[Int]], radius: Option[Int], chunkSize: Int, lag: Int = 0, model: ModelType, ols: OLSType) extends KrigingInterpolationMethod {
*/
class KrigingUniversal(points: Seq[PointFeature[Int]], radius: Option[Int], chunkSize: Int, lag: Int = 0, model: ModelType) extends KrigingInterpolationMethod {
  def createPredictor(): Point => (Double, Double) = {
    pointPredict => {
      val pointSize = points.size
      if (pointSize == 0)
        throw new IllegalArgumentException("No Points in the observation sequence");

      //OLS Estimation
      val attrArray = Array.ofDim[Double](pointSize, 6)
      cfor(0)(_ < pointSize, _ + 1) { row =>
        val s1 = points(row).geom.x
        val s2 = points(row).geom.y
        attrArray(row) = Array(1, s1, s2, s1 * s1, s1 * s2, s2 * s2)
      }
      val yMatrix: RealMatrix = MatrixUtils.createColumnRealMatrix(points.map(x => x.data.toDouble).toArray)
      val attrMatrix: RealMatrix = MatrixUtils.createRealMatrix(attrArray)
      val betaOLS: RealMatrix = new LUDecomposition(attrMatrix.transpose().multiply(attrMatrix)).getSolver.getInverse.multiply(attrMatrix.transpose()).multiply(yMatrix)

      val errorOLS = yMatrix.subtract(attrMatrix.multiply(betaOLS))

      //Covariance Estimation
      val sv = Semivariogram(points, radius, lag, model)

      //Full covariogram
      val covariogram: RealMatrix = getCovariogramMatrix(sv, points)
      val covarianceInverse: RealMatrix = new LUDecomposition(covariogram).getSolver.getInverse
      val sill: Double = getSill(sv, points, model)

      //GLS Estimation (Full matrix)
      val betaN: RealMatrix = new LUDecomposition(attrMatrix.transpose().multiply(covarianceInverse).multiply(attrMatrix)).getSolver.getInverse.multiply(attrMatrix.transpose()).multiply(covarianceInverse).multiply(yMatrix)

      //Prediction Set
      //TODO : Check if n >= k + 2 holds else reiterate the set generation process
      val predictionSet: Seq[PointFeature[Int]] = getPredictionSet(points, radius.get.toDouble, pointPredict)

      val covariogramSample: RealMatrix = getCovariogramMatrix(sv, predictionSet)
      val covariogramSampleInverse: RealMatrix = new LUDecomposition(covariogramSample).getSolver.getInverse
      val sillSample: Double = getSill(sv, points, model)
      val pointSampleSize = predictionSet.size
      val ySampleMatrix: RealMatrix = MatrixUtils.createColumnRealMatrix(predictionSet.map(x => x.data.toDouble).toArray)

      val cSampleMatrix: RealMatrix = MatrixUtils.createRealMatrix(Array.tabulate(1, pointSampleSize) { (_, i) => sill - sv(distance(pointPredict, predictionSet(i).geom)) })
      val attrSampleArray = Array.ofDim[Double](pointSampleSize, pointSampleSize)
      cfor(0)(_ < pointSampleSize, _ + 1) { row =>
        val s1 = predictionSet(row).geom.x
        val s2 = predictionSet(row).geom.y
        attrSampleArray(row) = Array(1, s1, s2, s1 * s1, s1 * s2, s2 * s2)
      }
      val attrSampleMatrix: RealMatrix = MatrixUtils.createRealMatrix(attrSampleArray)
      val errorSampleMatrix: RealMatrix = ySampleMatrix.subtract(attrSampleMatrix.multiply(betaN))

      val errorPoint: Double = cSampleMatrix.multiply(covariogramSampleInverse).multiply(errorSampleMatrix).getEntry(0, 0)
      val x0Array = Array.ofDim[Double](1, pointSize)
      val s1 = pointPredict.x
      val s2 = pointPredict.y
      x0Array(0) = Array(1, s1, s2, s1 * s1, s1 * s2, s2 * s2)
      val x0: RealMatrix = MatrixUtils.createRealMatrix(x0Array)
      val prediction: Double = x0.multiply(betaN).getEntry(0, 0) + errorPoint

      val part1: Double = sill - cSampleMatrix.multiply(covariogramSampleInverse).multiply(cSampleMatrix.transpose()).getEntry(0, 0)
      val part2_1: RealMatrix = x0.subtract(attrSampleMatrix.transpose().multiply(covariogramSampleInverse).multiply(cSampleMatrix.transpose())).transpose()
      val part2_2: RealMatrix = new LUDecomposition(attrSampleMatrix.transpose().multiply(covariogramSampleInverse).multiply(attrSampleMatrix)).getSolver.getInverse

      val part2_2_1: RealMatrix = new CholeskyDecomposition(attrSampleMatrix.transpose().multiply(covariogramSampleInverse).multiply(attrSampleMatrix)).getSolver.getInverse
      val part2_3: RealMatrix = x0.subtract(attrSampleMatrix.transpose().multiply(covariogramSampleInverse).multiply(cSampleMatrix.transpose()))

      val krigingVariance: Double = math.sqrt(part1 + part2_1.multiply(part2_2).multiply(part2_3).getEntry(0, 0))
      (prediction, krigingVariance)
    }
  }
}

class KrigingGeo(points: Seq[PointFeature[Int]], radius: Option[Int], chunkSize: Int, lag: Int = 0, model: ModelType) extends KrigingInterpolationMethod {
  def createPredictor(): Point => (Double, Double) = {
    pointPredict => {

      val pointSize = points.size
      if (pointSize == 0)
        throw new IllegalArgumentException("No Points in the observation sequence");

      val attrArray = Array.ofDim[Double](pointSize, 6)
      cfor(0)(_ < pointSize, _ + 1) { row =>
        val s1 = points(row).geom.x
        val s2 = points(row).geom.y
        attrArray(row) = Array(1, s1, s2, s1 * s1, s1 * s2, s2 * s2)
      }
      val yMatrix: RealMatrix = MatrixUtils.createColumnRealMatrix(points.map(x => x.data.toDouble).toArray)
      val attrMatrix: RealMatrix = MatrixUtils.createRealMatrix(attrArray)

      //1. OLS Estimate (Beta)
      val betaOLS: RealMatrix = new LUDecomposition(attrMatrix.transpose().multiply(attrMatrix)).getSolver.getInverse.multiply(attrMatrix.transpose()).multiply(yMatrix)
      val errorOLS = yMatrix.subtract(attrMatrix.multiply(betaOLS))

      //2. Empirical Variogram

      //3. Fit into a semivariogram

      //4. Construct a covariogram

      //5. Construct the covariance matrix

      //6. Beta (new) and residuals (new)

      //7. Generate new semivariogram

      //8. if(Threshold check) proceed, else reiterate steps 4 through 7

      //9. (beta, theta) is evaluated

      val prediction: Double = ???
      //val part2_2: RealMatrix = new LUDecomposition(attrSampleMatrix.transpose().multiply(covariogramSampleInverse).multiply(attrSampleMatrix)).getSolver.getInverse
      //val part2_2_1: RealMatrix = new CholeskyDecomposition(attrSampleMatrix.transpose().multiply(covariogramSampleInverse).multiply(attrSampleMatrix)).getSolver.getInverse

      val krigingVariance: Double = ???
      (prediction, krigingVariance)
    }
  }
}