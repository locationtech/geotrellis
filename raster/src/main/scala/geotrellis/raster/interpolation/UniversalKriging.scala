package geotrellis.raster.interpolation

import geotrellis.vector.PointFeature
import geotrellis.vector.Point
import geotrellis.vector.interpolation.{NonLinearSemivariogram, ModelType, Semivariogram}

import org.apache.commons.math3.linear._

import spire.syntax.cfor._

/**
 * @param points          Sample points for Universal Kriging model training
 * @param attrFunc        Attribute matrix transformation for a point (which decides how the point coordinates guide the pointData's value)
 * @param bandwidth       The maximum inter-point pair-distances which influence the prediction
 * @param model           The [[ModelType]] to be used for prediction
 */
class UniversalKriging(points: Array[PointFeature[Double]],
                       attrFunc: (Double, Double) => Array[Double],
                       bandwidth: Double,
                       model: ModelType) extends Kriging {
  /**
   * Overloaded constructor, for default attribute matrix generation
   */
  def this(points: Array[PointFeature[Double]], bandwidth: Double, model: ModelType) {
    this(points, (x, y) => Array(x, y, x * x, x * y, y * y), bandwidth, model)
  }

  /**
   * Universal Kriging training with the sample points
   * @param numberOfPoints  Number of points to be Kriged
   */
  protected def createPredictorInit(numberOfPoints: Int): (Double, Double) => (Double, Double) = {
    val n: Int = points.length
    if (n == 0)
      throw new IllegalArgumentException("No points in the training dataset")

    val unitCol = MatrixUtils.createColumnRealMatrix(Array.fill(n)(1))
    val attrMatrix =
      MatrixUtils.createRealMatrix(Array.tabulate(n)
      { i => Array(1.0) ++ attrFunc(points(i).geom.x, points(i).geom.y) })
    val attrSize: Int = attrMatrix.getColumnDimension - 1
    val scale: RealMatrix =
      new EigenDecomposition(
        MatrixUtils.createRealDiagonalMatrix(
          Array.tabulate(attrSize + 1)
          { i => absArray(attrMatrix.getColumn(i)).max }
        )
      ).getSolver.getInverse

    val attrMatrixScaled: RealMatrix = attrMatrix.multiply(scale)
    val ptData: RealMatrix = MatrixUtils.createColumnRealMatrix(points.map(x => x.data))

    val unscaledBetaOLS: RealMatrix =
      new SingularValueDecomposition(attrMatrixScaled)
        .getSolver.solve(ptData)

    val betaOLS: RealMatrix = scale.multiply(unscaledBetaOLS)
    val errorOLS: Array[Double] = ptData.subtract(attrMatrix.multiply(betaOLS)).getColumn(0)
    val pointsFitting: Array[PointFeature[Double]] =
      Array.tabulate(n)
      { row: Int => PointFeature(points(row).geom, errorOLS(row)) }

    val res: Semivariogram = NonLinearSemivariogram(pointsFitting, 0, 0, model)

    var covariogramMatrix: RealMatrix =
      unitCol.multiply(unitCol.transpose())
        .scalarMultiply(res.sill)
        .subtract(varianceMatrixGen(res, points))
        .add(MatrixUtils.createRealIdentityMatrix(n)
        .scalarMultiply(res.nugget))

    val rank: Int = new SingularValueDecomposition(covariogramMatrix).getRank

    if (rank < covariogramMatrix.getRowDimension)
      covariogramMatrix = covariogramMatrix.add(MatrixUtils.createRealIdentityMatrix(n).scalarMultiply(0.0001))

    //Inverse of C(the covariance matrix using the fitted semivariogram) utilizing Cholesky decomposition
    //for faster computations (after ensuring the matrix in invertible)
    val covariogramMatrixInv: RealMatrix =
      new LUDecomposition(
        new CholeskyDecomposition(covariogramMatrix).getL
      ).getSolver.getInverse

    val unscaledBetaN =
      new SingularValueDecomposition(
        covariogramMatrixInv.multiply(attrMatrixScaled)
      ).getSolver
        .solve(covariogramMatrixInv.multiply(ptData))

    val betaN = scale.multiply(unscaledBetaN)
    val residual: RealMatrix = ptData.subtract(attrMatrix.multiply(betaN))
    var i = 0

    (x: Double, y: Double) =>
      val pointPredict = Point(x, y)
      val distanceSortedInfo: Array[(Int, Double)] = getPointDistancesSorted(points, attrSize + 2, bandwidth, pointPredict)
      val distanceID: Array[Int] = distanceSortedInfo.map(_._1)
      val localCovarianceInv: RealMatrix =
        new SingularValueDecomposition(covariogramMatrix.getSubMatrix(distanceID, distanceID))
          .getSolver.getInverse
      val sortedDist: RealMatrix = MatrixUtils.createColumnRealMatrix(distanceSortedInfo.map(_._2))

      val localCovVector: RealMatrix =
        unitCol.getSubMatrix(distanceID, Array(0))
          .scalarMultiply(res.sill)
          .subtract(
            MatrixUtils.createRealMatrix(
              Array.tabulate(sortedDist.getRowDimension, 1)
              { (i, _) => res(sortedDist.getEntry(i,0)) }
            )
          )
      cfor(0)(_ < sortedDist.getRowDimension, _ + 1) { j: Int =>
        if (sortedDist.getEntry(j, 0) == 0)
          localCovVector.setEntry(j, 0, localCovVector.getEntry(j, 0) + res.nugget)
      }

      val curAttrVal: Array[Double] = Array(1.0) ++ attrFunc(x, y)
      val kPredict: Double = (
        MatrixUtils.createRowRealMatrix(curAttrVal)
          .multiply(betaN).getEntry(0,0)
          + localCovVector.transpose()
          .multiply(localCovarianceInv)
          .multiply(residual.getSubMatrix(distanceID,Array(0)))
          .getEntry(0,0)
        )

      val attrMatrixScaledSelect: RealMatrix =
        attrMatrixScaled.getSubMatrix(distanceID, (0 to attrMatrix.getColumnDimension - 1).toArray)
      val rankTemp: Int =
        new SingularValueDecomposition(
          attrMatrixScaledSelect.transpose().multiply(localCovarianceInv).multiply(attrMatrixScaledSelect)
        ).getRank

      val xtVinvXTemp: RealMatrix =
        attrMatrixScaledSelect.transpose().multiply(localCovarianceInv).multiply(attrMatrixScaledSelect)
      //X' * VInverse * X
      val xtVinvX: RealMatrix =
        if (rankTemp < attrSize + 1)
          scale.multiply(new SingularValueDecomposition(xtVinvXTemp)
            .getSolver.getInverse
          ).multiply(scale)
        else
          scale.multiply(new EigenDecomposition(xtVinvXTemp)
            .getSolver.getInverse
          ).multiply(scale)

      val kVarTemp: RealMatrix =
        MatrixUtils.createColumnRealMatrix(curAttrVal)
          .subtract(
            attrMatrix.getSubMatrix(distanceID, (0 to attrMatrix.getColumnDimension - 1).toArray)
              .transpose()
              .multiply(localCovarianceInv)
              .multiply(localCovVector)
          )

      val kVar: Double =
        math.sqrt(
          res.sill
            - localCovVector.transpose()
            .multiply(localCovarianceInv)
            .multiply(localCovVector).getEntry(0,0)
            + kVarTemp.transpose()
            .multiply(xtVinvX)
            .multiply(kVarTemp).getEntry(0,0)
        )
      i = i + 1

      (kPredict, kVar)
  }
}
