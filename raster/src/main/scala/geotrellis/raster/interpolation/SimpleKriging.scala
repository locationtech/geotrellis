package geotrellis.raster.interpolation

import geotrellis.vector.PointFeature
import geotrellis.vector.Point
import geotrellis.vector.interpolation.Semivariogram

import org.apache.commons.math3.linear._

import spire.syntax.cfor._

/**
 * @param points          Sample points for Simple Kriging model training
 * @param bandwidth       The maximum inter-point pair-distances which influence the prediction
 * @param sv              The fitted [[Semivariogram]] to be used for prediction
 */
class SimpleKriging(points: Array[PointFeature[Double]],
                    bandwidth: Double,
                    sv: Semivariogram) extends Kriging {
  /**
   * Simple Kriging training with the sample points
   * @param numberOfPoints  Number of points to be Kriged
   */
  protected def createPredictorInit(numberOfPoints: Int): (Double, Double) => (Double, Double) = {
    val n = points.length
    if (n == 0)
      throw new IllegalArgumentException("No points in the training dataset")

    val unitCol = MatrixUtils.createColumnRealMatrix(Array.fill(n)(1))
    val covariogramMatrix: RealMatrix =
      unitCol.multiply(unitCol.transpose())
        .scalarMultiply(sv.sill)
        .subtract(varianceMatrixGen(sv, points))
        .add(MatrixUtils.createRealIdentityMatrix(n).scalarMultiply(sv.nugget))
    val ptData = MatrixUtils.createColumnRealMatrix(points.map(x => x.data))

    (x: Double, y: Double) =>
      val pointPredict: Point = Point(x, y)
      val distanceSortedInfo = getPointDistancesSorted(points, 3, bandwidth, pointPredict)
      val distanceID: Array[Int] = distanceSortedInfo.map(_._1)

      val localCovariance =
        new EigenDecomposition(
          covariogramMatrix.getSubMatrix(distanceID, distanceID)
        ).getSolver.getInverse

      val distSorted = MatrixUtils.createColumnRealMatrix(distanceSortedInfo.map(_._2))

      val covVec: RealMatrix =
        unitCol.getSubMatrix(distanceID, Array(0))
          .scalarMultiply(sv.sill)
          .subtract(
            MatrixUtils.createRealMatrix(
              Array.tabulate(distSorted.getRowDimension, 1)
              { (i, _) => sv(distSorted.getEntry(i,0)) }
            )
          )

      cfor(0)(_ < distSorted.getRowDimension, _ + 1) { j: Int =>
        if (distSorted.getEntry(j, 0) == 0)
          covVec.setEntry(j, 0, covVec.getEntry(j, 0) + sv.nugget)
      }

      val mu: Double = points.foldLeft(0.0)(_ + _.data) / n
      val kTemp: RealMatrix = covVec.transpose().multiply(localCovariance)
      val kPredict =
        mu + kTemp.multiply(
          ptData.getSubMatrix(distanceID, Array(0))
            .subtract(
              unitCol.getSubMatrix(distanceID, Array(0))
                .scalarMultiply(mu)
            )
        ).getEntry(0, 0)
      val kVar = math.sqrt(sv.sill - kTemp.multiply(covVec).getEntry(0, 0))
      (kPredict, kVar)
  }
}
