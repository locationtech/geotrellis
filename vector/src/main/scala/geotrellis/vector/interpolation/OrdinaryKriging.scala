/*
* Copyright (c) 2015 Azavea.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

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
    val n = points.length
    if (n == 0)
      throw new IllegalArgumentException("No points in the training dataset")

    val unitCol = MatrixUtils.createColumnRealMatrix(Array.fill(n)(1))
    val ptData = MatrixUtils.createColumnRealMatrix(points.map(x => x.data))

    val eyen = MatrixUtils.createRealIdentityMatrix(n)
    val covariogramMatrix =
      unitCol.multiply(unitCol.transpose())
        .scalarMultiply(sv.sill)
        .subtract(varianceMatrixGen(sv, points))
        .add(eyen.scalarMultiply(sv.nugget))
    val muPreComp: RealMatrix =
      try {
        unitCol.transpose()
          .multiply(new LUDecomposition(covariogramMatrix)
                      .getSolver.getInverse)
      }
      catch {
        case _: Exception =>
          unitCol.transpose()
            .multiply(new LUDecomposition(covariogramMatrix
                          .add(eyen.scalarMultiply(0.0000001)))
                      .getSolver.getInverse)
      }

    val mu: Double = muPreComp.multiply(ptData).getEntry(0, 0) / muPreComp.multiply(unitCol).getEntry(0, 0)
    val residual: RealMatrix = ptData.subtract(unitCol.scalarMultiply(mu))

    (x: Double, y: Double) =>
      val pointPredict = Point(x, y)
      val distanceSortedInfo: Array[(Int, Double)] =
        getPointDistancesSorted(points, 3, bandwidth,  pointPredict)
      val distanceID: Array[Int] = distanceSortedInfo.map(_._1)

      val localCovariance =
        new LUDecomposition(covariogramMatrix.getSubMatrix(distanceID, distanceID))
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

      cfor(0)(_ < distSorted.getRowDimension, _ + 1) { i: Int =>
        if (distSorted.getEntry(i, 0) == 0)
          localCovVec.setEntry(i, 0, localCovVec.getEntry(i, 0) + sv.nugget)
      }

      val distSortedUnit =
        MatrixUtils.createColumnRealMatrix(Array.fill(distanceSortedInfo.length)(1))
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
