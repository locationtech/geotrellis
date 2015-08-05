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

package geotrellis.vector.interpolation
import geotrellis.vector._
import org.apache.commons.math3.linear._
import spire.syntax.cfor._

/**
 * @param points          Sample points for training
 * @param bandwidth       The maximum inter-point pair-distances which influence the prediction
 * @param sv              [[Semivariogram]] to be used for Kriging prediction
 */
class KrigingSimple(points: Array[PointFeature[Double]], bandwidth: Double, sv: Semivariogram) extends KrigingVectorBase {

  /**
   * Simple Kriging Prediction for a single point
   */
  def createPredictor(): Point => (Double, Double) = {
    P: Point => predict(Array(P))(0)
  }

  /**
   * Simple Kriging Prediction for an Array of points
   * @param pointMatrix Points to be Kriged
   * @return            Tuples of (krigedValues, krigedVariance) for each of the kriged points
   */
  def predict(pointMatrix: Array[Point]): Array[(Double, Double)] = {
    val n: Int = points.length
    if (n == 0)
      throw new IllegalArgumentException("No points in the training dataset")

    val unitCol: RealMatrix = MatrixUtils.createColumnRealMatrix(Array.fill(n)(1))
    //Covariogram Matrix
    val C: RealMatrix = unitCol.multiply(unitCol.transpose()).scalarMultiply(sv.sill).subtract(varianceMatrixGen(sv, points)).add(MatrixUtils.createRealIdentityMatrix(n).scalarMultiply(sv.nugget))
    val pointValue: RealMatrix = MatrixUtils.createColumnRealMatrix(points.map(x => x.data))
    val krigingPrediction: Array[(Double, Double)] = Array.ofDim[(Double, Double)](pointMatrix.length)
    cfor(0)(_ < pointMatrix.length, _ + 1) { i: Int =>
      val pointPredict: Point = pointMatrix(i)
      val distanceSortedSimple: Array[(Int, Double)] = getPointDistancesSorted(points, 3, bandwidth, pointPredict)
      val distanceID: Array[Int] = distanceSortedSimple.map(_._1)
      //Local Covariances
      val CC: RealMatrix = new EigenDecomposition(C.getSubMatrix(distanceID, distanceID)).getSolver.getInverse
      val d: RealMatrix = MatrixUtils.createColumnRealMatrix(distanceSortedSimple.map(_._2))
      //Local Covariance Vector
      val covVec: RealMatrix = unitCol.getSubMatrix(distanceID, Array(0)).scalarMultiply(sv.sill).subtract(MatrixUtils.createRealMatrix(Array.tabulate(d.getRowDimension, 1){(i, _) => sv(d.getEntry(i,0))}))
      cfor(0)(_ < d.getRowDimension, _ + 1) { j: Int =>
        if (d.getEntry(j, 0) == 0)
          covVec.setEntry(j, 0, covVec.getEntry(j, 0) + sv.nugget)
      }
      val mu: Double = points.foldLeft(0.0)(_ + _.data) / n
      val kTemp: RealMatrix = covVec.transpose().multiply(CC)
      val kPredict = mu + kTemp.multiply(pointValue.getSubMatrix(distanceID, Array(0)).subtract(unitCol.getSubMatrix(distanceID, Array(0)).scalarMultiply(mu))).getEntry(0, 0)
      val kVar = math.sqrt(sv.sill - kTemp.multiply(covVec).getEntry(0, 0))
      krigingPrediction(i) = (kPredict, kVar)
    }
    krigingPrediction
  }
}

/**
 * @param points          Sample points for training
 * @param bandwidth       The maximum inter-point pair-distances which influence the prediction
 * @param sv              [[Semivariogram]] to be used for Kriging prediction
 */
class KrigingOrdinary(points: Array[PointFeature[Double]], bandwidth: Double, sv: Semivariogram) extends KrigingVectorBase {

  /**
   * Ordinary Kriging Prediction for a single point
   */
  def createPredictor(): Point => (Double, Double) = {
    P: Point => predict(Array(P))(0)
  }

  /**
   * Ordinary Kriging Prediction for an Array of points
   * @param pointMatrix Points to be Kriged
   * @return            Tuples of (krigedValues, krigedVariance) for each of the kriged points
   */
  def predict(pointMatrix: Array[Point]): Array[(Double, Double)] = {
    val n: Int = points.length
    if (n == 0)
      throw new IllegalArgumentException("No points in the training dataset")

    val unitCol: RealMatrix = MatrixUtils.createColumnRealMatrix(Array.fill(n)(1))
    val pointValue: RealMatrix = MatrixUtils.createColumnRealMatrix(points.map(x => x.data))

    //Covariogram Matrix
    var C: RealMatrix = unitCol.multiply(unitCol.transpose()).scalarMultiply(sv.sill).subtract(varianceMatrixGen(sv, points)).add(MatrixUtils.createRealIdentityMatrix(n).scalarMultiply(sv.nugget))
    val rank: Int = new SingularValueDecomposition(C).getRank
    if (rank < C.getRowDimension)
      C = C.add(MatrixUtils.createRealIdentityMatrix(n).scalarMultiply(0.0000001))
    val muPreComp: RealMatrix = unitCol.transpose().multiply(new EigenDecomposition(C).getSolver.getInverse)
    val mu: Double = muPreComp.multiply(pointValue).getEntry(0, 0) / muPreComp.multiply(unitCol).getEntry(0, 0)
    val Residual: RealMatrix = pointValue.subtract(unitCol.scalarMultiply(mu))
    val krigingPrediction: Array[(Double, Double)] = Array.ofDim[(Double, Double)](pointMatrix.length)
    cfor(0)(_ < pointMatrix.length, _ + 1) { i: Int =>
      val pointPredict: Point = pointMatrix(i)
      val distanceSortedOrdinary: Array[(Int, Double)] = getPointDistancesSorted(points, 3, bandwidth,  pointPredict)
      val distanceID: Array[Int] = distanceSortedOrdinary.map(_._1)
      //Local Covariogrances
      val CC: RealMatrix = new EigenDecomposition(C.getSubMatrix(distanceID, distanceID)).getSolver.getInverse
      val d: RealMatrix = MatrixUtils.createColumnRealMatrix(distanceSortedOrdinary.map(_._2))
      //Local Covariance Vector
      val covVec: RealMatrix = unitCol.getSubMatrix(distanceID, Array(0)).scalarMultiply(sv.sill).subtract(MatrixUtils.createRealMatrix(Array.tabulate(d.getRowDimension, 1){(i, _) => sv(d.getEntry(i,0))}))
      cfor(0)(_ < d.getRowDimension, _ + 1) { j: Int =>
        if (d.getEntry(j, 0) == 0)
          covVec.setEntry(j, 0, covVec.getEntry(j, 0) + sv.nugget)
      }
      val Z: RealMatrix = MatrixUtils.createColumnRealMatrix(Array.fill(distanceSortedOrdinary.length)(1))
      val scalarDenom: Double = Z.transpose().multiply(CC).multiply(Z).getEntry(0,0)
      val scalarNum: Double = 1 - Z.transpose().multiply(CC).multiply(covVec).getEntry(0,0)
      val kPredict = mu + covVec.transpose().scalarAdd(scalarNum/scalarDenom).multiply(CC.multiply(Residual.getSubMatrix(distanceID, Array(0)))).getEntry(0,0)
      val kVar = math.sqrt(sv.sill - covVec.transpose().multiply(CC).multiply(covVec).getEntry(0,0) + math.pow(scalarNum,2)/scalarDenom)
      krigingPrediction(i) = (kPredict, kVar)
    }
    krigingPrediction
  }
}

/**
 * @param points          Sample points for training
 * @param attributeSample Sample points' attribute matrix (which decides how the point coordinates guide the pointData's value)
 * @param attribute       Prediction points' attribute matrix
 * @param bandwidth       The maximum inter-point pair-distances which influence the prediction
 * @param model           The [[ModelType]] to be used for prediction
 */
class KrigingUniversal(points: Array[PointFeature[Double]], attributeSample: Array[Array[Double]], attribute: Array[Array[Double]], bandwidth: Double, model: ModelType) extends KrigingVectorBase {
  /**
   * Universal Kriging Prediction for a single point
   */
  def createPredictor(): Point => (Double, Double) = {
    P: Point => predict(Array(P))(0)
  }

  /**
   * Universal Kriging Prediction for an Array of points
   * @param pointMatrix Points to be Kriged
   * @return            Tuples of (krigedValues, krigedVariance) for each of the kriged points
   */
  def predict(pointMatrix: Array[Point]): Array[(Double, Double)] = {
    val n: Int = points.length
    if (n == 0)
      throw new IllegalArgumentException("No points in the training dataset")

    val k: Int = attributeSample(0).length
    val unitCol: RealMatrix = MatrixUtils.createColumnRealMatrix(Array.fill(n)(1))
    val XX: RealMatrix = MatrixUtils.createRealMatrix(Array.tabulate(n){ i => Array(1.0) ++ attributeSample(i) })
    val S: RealMatrix = new EigenDecomposition(MatrixUtils.createRealDiagonalMatrix(Array.tabulate(k+1){i => absArray(XX.getColumn(i)).max})).getSolver.getInverse
    val Z: RealMatrix = XX.multiply(S)
    val y0: RealMatrix = MatrixUtils.createColumnRealMatrix(points.map(x => x.data))

    val bbOLS: RealMatrix = new SingularValueDecomposition(Z).getSolver.solve(y0)
    val bOLS: RealMatrix = S.multiply(bbOLS)
    val errorOLS: Array[Double] = y0.subtract(XX.multiply(bOLS)).getColumn(0)
    val pointsFitting: Array[PointFeature[Double]] = Array.tabulate(n) { row: Int => PointFeature(points(row).geom, errorOLS(row)) }
    val res: Semivariogram = NonLinearSemivariogram(pointsFitting, 0, 0, model)

    //Covariogram Matrix
    var C: RealMatrix = unitCol.multiply(unitCol.transpose()).scalarMultiply(res.sill).subtract(varianceMatrixGen(res, points)).add(MatrixUtils.createRealIdentityMatrix(n).scalarMultiply(res.nugget))
    val rank: Int = new SingularValueDecomposition(C).getRank
    if (rank < C.getRowDimension)
      C = C.add(MatrixUtils.createRealIdentityMatrix(n).scalarMultiply(0.0001))
    //Inverse of C(the covariance matrix using the fitted semivariogram) using Cholesky decomposition for faster computations (after ensuring the matrix in invertible)
    val AA: RealMatrix = new LUDecomposition(new CholeskyDecomposition(C).getL).getSolver.getInverse
    val bbN = new SingularValueDecomposition(AA.multiply(Z)).getSolver.solve(AA.multiply(y0))
    val bN = S.multiply(bbN)
    val N: Int = pointMatrix.length
    val XXNew: RealMatrix = MatrixUtils.createRealMatrix(Array.tabulate(N){ i => Array(1.0) ++ attribute(i)})
    val Residual: RealMatrix = y0.subtract(XX.multiply(bN))
    val krigingPrediction: Array[(Double, Double)] = Array.ofDim[(Double, Double)](pointMatrix.length)
    cfor(0)(_ < N, _ + 1) { i: Int =>
      val distanceSortedUniversal: Array[(Int, Double)] = getPointDistancesSorted(points, k+2, bandwidth, pointMatrix(i))
      val distanceID: Array[Int] = distanceSortedUniversal.map(_._1)
      //Local Covariances
      val CC: RealMatrix = new SingularValueDecomposition(C.getSubMatrix(distanceID, distanceID)).getSolver.getInverse
      val d: RealMatrix = MatrixUtils.createColumnRealMatrix(distanceSortedUniversal.map(_._2))
      val c: RealMatrix = unitCol.getSubMatrix(distanceID, Array(0)).scalarMultiply(res.sill).subtract(MatrixUtils.createRealMatrix(Array.tabulate(d.getRowDimension, 1){(i, _) => res(d.getEntry(i,0))}))
      cfor(0)(_ < d.getRowDimension, _ + 1) { j: Int =>
        if (d.getEntry(j, 0) == 0)
          c.setEntry(j, 0, c.getEntry(j, 0) + res.nugget)
      }
      val kPredict: Double = MatrixUtils.createRowRealMatrix(XXNew.getRow(i)).multiply(bN).getEntry(0,0) + c.transpose().multiply(CC).multiply(Residual.getSubMatrix(distanceID,Array(0))).getEntry(0,0)
      val ZZ: RealMatrix = Z.getSubMatrix(distanceID, Array.tabulate(XX.getColumnDimension){i => i})
      val rankTemp: Int = new SingularValueDecomposition(ZZ.transpose().multiply(CC).multiply(ZZ)).getRank
      val B: RealMatrix =
        if (rankTemp < k + 1)
          S.multiply(new SingularValueDecomposition(ZZ.transpose().multiply(CC).multiply(ZZ)).getSolver.getInverse).multiply(S)
        else
          S.multiply(new EigenDecomposition(ZZ.transpose().multiply(CC).multiply(ZZ)).getSolver.getInverse).multiply(S)
      val kVarTemp: RealMatrix = MatrixUtils.createColumnRealMatrix(XXNew.getRow(i)).subtract(XX.getSubMatrix(distanceID, Array.tabulate(XX.getColumnDimension){i => i}).transpose().multiply(CC).multiply(c))
      val kVar: Double = math.sqrt(res.sill - c.transpose().multiply(CC).multiply(c).getEntry(0,0) + kVarTemp.transpose().multiply(B).multiply(kVarTemp).getEntry(0,0))
      krigingPrediction(i) = (kPredict, kVar)
    }
    krigingPrediction
  }
}

/**
 * @param points          Sample points for training
 * @param attributeSample Sample points' attribute matrix (which decides how the point coordinates guide the pointData's value)
 * @param attribute       Prediction points' attribute matrix
 * @param bandwidth       The maximum inter-point pair-distances which influence the prediction
 * @param model           The [[ModelType]] to be used for prediction
 */
class KrigingGeo(points: Array[PointFeature[Double]], attributeSample: Array[Array[Double]], attribute: Array[Array[Double]], bandwidth: Double, model: ModelType) extends KrigingVectorBase {

  /**
   * Geostatistical Kriging Prediction for a single point
   */
  def createPredictor(): Point => (Double, Double) = {
    P: Point => predict(Array(P))(0)
  }

  protected def varianceMatrixGenOld(sv: Semivariogram, distanceM: RealMatrix): RealMatrix = {
    val n: Int = distanceM.getRowDimension
    val varMatrix: RealMatrix = MatrixUtils.createRealMatrix(n, n)

    //Variance shifts to sill, if distance>range
    cfor(0)(_ < n, _ + 1) { i =>
      cfor(0)(_ < n, _ + 1) { j =>
        distanceM.setEntry(i,j,math.min(distanceM.getEntry(i,j), sv.range))
        varMatrix.setEntry(i,j,sv(distanceM.getEntry(i,j)))
      }
    }
    varMatrix
  }

  /**
   * Computes the pairwise distance as a matrix
   */
  protected def distanceMatrix(xy: RealMatrix): RealMatrix = {
    def repmat(mat: RealMatrix, n: Int, m: Int): RealMatrix = {
      val rd: Int = mat.getRowDimension
      val cd: Int = mat.getColumnDimension
      val d: Array[Array[Double]] = Array.ofDim[Double](n * rd, m * cd)
      cfor(0)(_ < n*rd, _ + 1) { r =>
        cfor(0)(_ < m*cd, _ + 1) { c =>
          d(r)(c) = mat.getEntry(r % rd, c % cd)
        }
      }
      MatrixUtils.createRealMatrix(d)
    }
    val n: Int = xy.getRowDimension
    val xyT: RealMatrix = xy.transpose()
    val xy2: RealMatrix = MatrixUtils.createRowRealMatrix(Array.tabulate(n) { i => math.pow(xyT.getEntry(0,i), 2) + math.pow(xyT.getEntry(1,i), 2)})
    val ret: RealMatrix = repmat(xy2, n, 1).add(repmat(xy2.transpose(), 1, n)).subtract(xy.multiply(xy.transpose).scalarMultiply(2))
    cfor(0)(_ < n, _ + 1) { i =>
      ret.setEntry(i,i,0)
      cfor(0)(_ < n, _ + 1) { j =>
        if (i!=j) ret.setEntry(i,j,math.sqrt(ret.getEntry(i,j)))
      }
    }
    ret
  }

  /**
   * Geostatistical Kriging Prediction for an Array of points
   * @param pointMatrix Points to be Kriged
   * @return            Tuples of (krigedValues, krigedVariance) for each of the kriged points
   */
  def predict(pointMatrix: Array[Point]): Array[(Double, Double)] = {
    val n: Int = points.length
    if (n == 0)
      throw new IllegalArgumentException("No points in the training dataset")

    val k: Int = attributeSample(0).length
    val unitCol: RealMatrix = MatrixUtils.createColumnRealMatrix(Array.fill(n)(1))
    val attrMatrix: RealMatrix = MatrixUtils.createRealMatrix(Array.tabulate(n){ i => Array(1.0) ++ attributeSample(i) })
    val scale: RealMatrix = new EigenDecomposition(MatrixUtils.createRealDiagonalMatrix(Array.tabulate(k+1){i => absArray(attrMatrix.getColumn(i)).max})).getSolver.getInverse
    val Z: RealMatrix = attrMatrix.multiply(scale)
    val y0: RealMatrix = MatrixUtils.createColumnRealMatrix(points.map(x => x.data))
    val bbOLS: RealMatrix = new SingularValueDecomposition(Z).getSolver.solve(y0)
    val bOLS: RealMatrix = scale.multiply(bbOLS)
    val errorOLS: RealMatrix = y0.subtract(attrMatrix.multiply(bOLS))
    val pointsFitting: Array[PointFeature[Double]] = Array.tabulate(n) {row: Int => PointFeature(points(row).geom, errorOLS.getEntry(row, 0)) }
    var res: Semivariogram = NonLinearSemivariogram(pointsFitting, 0, 0, model)
    var delta: Double = 1.0
    var counter = 0
    var bEval = bOLS
    while (delta > 0.001) {
      counter = counter + 1
      val checking = varianceMatrixGen(res, points)
      var C: RealMatrix = unitCol.multiply(unitCol.transpose()).scalarMultiply(res.sill).subtract(checking).add(MatrixUtils.createRealIdentityMatrix(n).scalarMultiply(res.nugget))
      val rank: Int = new SingularValueDecomposition(C).getRank
      if (rank < C.getRowDimension)
        C = C.add(MatrixUtils.createRealIdentityMatrix(n).scalarMultiply(0.0001))
      val CInv = new SingularValueDecomposition(new CholeskyDecomposition(C).getL).getSolver.solve(MatrixUtils.createRealIdentityMatrix(n))
      val bb = new SingularValueDecomposition(CInv.multiply(Z)).getSolver.solve(CInv.multiply(y0))
      val b = scale.multiply(bb)
      val errorIter = y0.subtract(attrMatrix.multiply(b))
      val pointsFittingIter = Array.tabulate(n) {row: Int => PointFeature(points(row).geom, errorIter.getEntry(row, 0)) }
      val process: Array[Double] = b.subtract(bEval).getColumn(0)
      bEval = b
      delta = absArray(Array.tabulate(process.length){i => math.abs(process(i))/bEval.getEntry(i,0)}).max
      if (delta > 0.0001) {
        res = NonLinearSemivariogram(pointsFittingIter, 0, 0, model)
        //println("Delta = " + delta)
        if(counter > 100)
          delta = 0.00001
      }
    }
    val C: RealMatrix = unitCol.multiply(unitCol.transpose()).scalarMultiply(res.sill).subtract(varianceMatrixGen(res, points)).add(MatrixUtils.createRealIdentityMatrix(n).scalarMultiply(res.nugget))
    val N: Int = pointMatrix.length
    val attrMatrixNew: RealMatrix = MatrixUtils.createRealMatrix(Array.tabulate(N){ i => Array(1.0) ++ attribute(i) })
    val prediction: Array[(Double, Double)] = Array.ofDim[(Double, Double)](pointMatrix.length)
    val residual: RealMatrix = y0.subtract(attrMatrix.multiply(bEval))
    cfor(0)(_ < N, _ + 1) { i: Int =>
      val pointPredict: Point = pointMatrix(i)
      val distanceSorted: Array[(Int, Double)] = getPointDistancesSorted(points, k+2, bandwidth, pointPredict)
      val distanceID: Array[Int] = distanceSorted.map(_._1)
      val CC: RealMatrix = new SingularValueDecomposition(C.getSubMatrix(distanceID, distanceID)).getSolver.getInverse
      val d: RealMatrix = MatrixUtils.createColumnRealMatrix(distanceSorted.map(_._2))
      val c: RealMatrix = unitCol.getSubMatrix(distanceID, Array(0)).scalarMultiply(res.sill).subtract(MatrixUtils.createRealMatrix(Array.tabulate(distanceID.length, 1){(j, _) => res(d.getEntry(j,0))}))
      cfor(0)(_ < distanceID.length, _ + 1) { j: Int =>
        if (d.getEntry(j, 0) == 0)
          c.setEntry(j, 0, c.getEntry(j, 0) + res.nugget)
      }
      val kPredict: Double = MatrixUtils.createRowRealMatrix(attrMatrixNew.getRow(i)).multiply(bEval).getEntry(0,0) + c.transpose().multiply(CC).multiply(residual.getSubMatrix(distanceID,Array(0))).getEntry(0,0)
      val colID: Array[Int] = Array.tabulate(attrMatrix.getColumnDimension){ j => j }
      val W: RealMatrix = attrMatrix.getSubMatrix(distanceID, colID)
      val ZZ: RealMatrix = Z.getSubMatrix(distanceID, colID)
      val rankTemp: Int = new SingularValueDecomposition(ZZ.transpose().multiply(CC).multiply(ZZ)).getRank
      val B: RealMatrix =
        if (rankTemp < k + 1)
          scale.multiply(new SingularValueDecomposition(ZZ.transpose().multiply(CC).multiply(ZZ)).getSolver.getInverse).multiply(scale)
        else
          scale.multiply(new EigenDecomposition(ZZ.transpose().multiply(CC).multiply(ZZ)).getSolver.getInverse).multiply(scale)
      val kVarTemp = MatrixUtils.createColumnRealMatrix(attrMatrixNew.transpose().getColumn(i)).subtract(W.transpose().multiply(CC).multiply(c))
      val kVar: Double = math.sqrt(res.sill - c.transpose().multiply(CC).multiply(c).getEntry(0,0) + kVarTemp.transpose().multiply(B).multiply(kVarTemp).getEntry(0,0))
      prediction(i) = (kPredict, kVar)
    }
    prediction
  }
}
