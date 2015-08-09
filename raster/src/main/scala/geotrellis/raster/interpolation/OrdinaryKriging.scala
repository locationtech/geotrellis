package geotrellis.raster.interpolation

import geotrellis.vector.PointFeature
import geotrellis.vector.Point
import geotrellis.vector.interpolation.Semivariogram

import org.apache.commons.math3.linear._

import spire.syntax.cfor._

/**
 * @param points          Sample points for Ordinary Kriging model training
 * @param bandwidth       The maximum inter-point pair-distances which influence the prediction
 * @param sv              The fitted [[Semivariogram]] to be used for prediction
 */
class OrdinaryKriging(points: Array[PointFeature[Double]],
                      bandwidth: Double,
                      sv: Semivariogram) extends Kriging {

  /**
   * Ordinary Kriging training with the sample points
   * @param numberOfPoints  Number of points to be Kriged
   */
  protected def createPredictorInit(numberOfPoints: Int): (Double, Double) => (Double, Double) = {
    val n: Int = points.length
    if (n == 0)
      throw new IllegalArgumentException("No points in the training dataset")

    val unitCol: RealMatrix = MatrixUtils.createColumnRealMatrix(Array.fill(n)(1))
    val ptData: RealMatrix = MatrixUtils.createColumnRealMatrix(points.map(x => x.data))

    var covariogramMatrix =
      unitCol.multiply(unitCol.transpose())
        .scalarMultiply(sv.sill)
        .subtract(varianceMatrixGen(sv, points))
        .add(
          MatrixUtils.createRealIdentityMatrix(n)
            .scalarMultiply(sv.nugget)
        )

    val rank: Int = new SingularValueDecomposition(covariogramMatrix).getRank
    if (rank < covariogramMatrix.getRowDimension)
      covariogramMatrix = covariogramMatrix.add(MatrixUtils.createRealIdentityMatrix(n).scalarMultiply(0.0000001))

    val muPreComp: RealMatrix =
      unitCol.transpose()
        .multiply(
          new EigenDecomposition(covariogramMatrix)
            .getSolver.getInverse
        )

    val mu: Double = muPreComp.multiply(ptData).getEntry(0, 0) / muPreComp.multiply(unitCol).getEntry(0, 0)
    val residual: RealMatrix = ptData.subtract(unitCol.scalarMultiply(mu))

    (x: Double, y: Double) =>
      val pointPredict = Point(x, y)
      val distanceSortedInfo: Array[(Int, Double)] = getPointDistancesSorted(points, 3, bandwidth,  pointPredict)
      val distanceID: Array[Int] = distanceSortedInfo.map(_._1)

      val localCovariance =
        new EigenDecomposition(covariogramMatrix.getSubMatrix(distanceID, distanceID))
          .getSolver.getInverse

      val distSorted = MatrixUtils.createColumnRealMatrix(distanceSortedInfo.map(_._2))
      val localCovVec: RealMatrix =
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
          localCovVec.setEntry(j, 0, localCovVec.getEntry(j, 0) + sv.nugget)
      }

      val distSortedUnit = MatrixUtils.createColumnRealMatrix(Array.fill(distanceSortedInfo.length)(1))
      val scalarDenom: Double =
        distSortedUnit.transpose()
          .multiply(localCovariance)
          .multiply(distSortedUnit)
          .getEntry(0,0)
      val scalarNum: Double =
        1 - distSortedUnit.transpose()
          .multiply(localCovariance)
          .multiply(localCovVec)
          .getEntry(0,0)
      val kPredict =
        mu + localCovVec.transpose()
          .scalarAdd(scalarNum/scalarDenom)
          .multiply(
            localCovariance.multiply(residual.getSubMatrix(distanceID, Array(0)))
          ).getEntry(0,0)
      val kVar =
        math.sqrt(
          sv.sill
            - localCovVec.transpose()
            .multiply(localCovariance)
            .multiply(localCovVec).getEntry(0,0)
            + math.pow(scalarNum,2)/scalarDenom
        )
      (kPredict, kVar)
  }
}
