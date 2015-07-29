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
 * @author Vishal Anand
 */

trait KrigingVectorInterpolationMethod{
  def createPredictor(): Point => (Double, Double)
  def predict(pointMatrix: Array[Point]): Array[(Double, Double)]

  private def distance(p1: Point, p2: Point) = math.abs(math.sqrt(math.pow(p1.x - p2.x, 2) + math.pow(p1.y - p2.y, 2)))

  protected def varianceMatrixGen(sv: Semivariogram, distanceM: RealMatrix): RealMatrix = {
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
   * Returns a cluster of distances of pointPredict from the Sample Point Set along with the indices
   */
  protected def getDistances(points: Seq[PointFeature[Double]], point: Point): Array[(PointFeature[Double], Int)] = {
    var distanceID: Array[(PointFeature[Double], Int)] = Array()
    cfor(0)(_ < points.length, _ + 1) { j: Int =>
      distanceID = distanceID :+(PointFeature(points(j).geom, distance(points(j).geom, point)), j)
    }
    distanceID
  }

  /**
   * Returns the indices of points close to the point for prediction within the given bandwidth
   * In case the number of points < minPoints; it returns the closest minPoints number of points
   */
  protected def getPointDistances(points: Seq[PointFeature[Double]], distanceID: Array[(PointFeature[Double], Int)], minPoints: Int, bandwidth: Double, point: Point): Array[Int] = {

    var sequenceID: Array[Int] = Array()
    cfor(0)(_ < points.length, _ + 1) { j: Int =>
      val curDist = distance(points(j).geom, point)
      if (curDist < bandwidth)
        sequenceID = sequenceID :+ j
    }
    if (sequenceID.length < minPoints) {
      var result: Array[Int] = Array()
      distanceID.sortWith((f, s) => f._1.data < s._1.data)
      cfor(0)(_ < minPoints, _ + 1) { i =>
        result = result :+ distanceID(i)._2
      }
      result
    }
    else
      sequenceID
  }

  /**
   * Returns the absolute values of a given array
   */
  protected def absArray(arr: Array[Double]): Array[Double] = {
    cfor(0)(_ < arr.length, _ + 1) { i =>
      arr(i) = math.abs(arr(i))
    }
    arr
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
}

/**
 * @param points          Sample points for training
 * @param bandwidth       The maximum inter-point pair-distances which influence the prediction
 * @param sv              [[Semivariogram]] to be used for Kriging prediction
 */
class KrigingSimple(points: Seq[PointFeature[Double]], bandwidth: Double, sv: Semivariogram) extends KrigingVectorInterpolationMethod {

  /**
   * Simple Kriging Prediction for a single point
   */
  def createPredictor(): Point => (Double, Double) = {
    P: Point => predict(Array(P))(0)
  }

  /**
   * Simple Kriging Prediction for an Array of points
   * @param pointMatrix Points to be Krigred
   * @return            Tuples of (krigedValues, krigedVariance) for each of the kriged points
   */
  def predict(pointMatrix: Array[Point]): Array[(Double, Double)] = {
    val n: Int = points.length
    val UCol: RealMatrix = MatrixUtils.createColumnRealMatrix(Array.fill(n)(1))
    val prediction: Array[(Double, Double)] = Array.ofDim[(Double, Double)](pointMatrix.length)
    val VMatrix: RealMatrix = MatrixUtils.createColumnRealMatrix(points.map(x => x.data).toArray)
    val xy: RealMatrix = MatrixUtils.createRealMatrix(Array.tabulate(points.length, 2) {
      (i, j) => {
        if (j == 0) points(i).geom.x
        else points(i).geom.y
      }
    })
    val distances: RealMatrix = distanceMatrix(xy)
    //val (range: Double, sill: Double, nugget: Double) = (svParam(0) ,svParam(1) ,svParam(2))
    val (range: Double, sill: Double, nugget: Double) = (sv.range, sv.sill, sv.nugget)
    //Covariogram Matrix
    val C: RealMatrix = UCol.multiply(UCol.transpose()).scalarMultiply(sill).subtract(varianceMatrixGen(sv, distances)).add(MatrixUtils.createRealIdentityMatrix(n).scalarMultiply(nugget))
    cfor(0)(_ < pointMatrix.length, _ + 1) { i: Int =>
      val pointPredict: Point = pointMatrix(i)
      val distanceSeq: Array[(PointFeature[Double], Int)] = getDistances(points, pointPredict)
      val distanceID: Array[Int] = getPointDistances(points, distanceSeq, 3, bandwidth, pointPredict)
      val distanceFromSample: RealMatrix = MatrixUtils.createColumnRealMatrix(Array.tabulate(n) { i => distanceSeq(i)._1.data })
      //Local Covariances
      val CC: RealMatrix = new EigenDecomposition(C.getSubMatrix(distanceID, distanceID)).getSolver.getInverse
      val d: RealMatrix = distanceFromSample.getSubMatrix(distanceID, Array(0))
      //Local Covariance Vector
      val covVec: RealMatrix = UCol.getSubMatrix(distanceID, Array(0)).scalarMultiply(sill).subtract(MatrixUtils.createRealMatrix(Array.tabulate(d.getRowDimension, 1){(i, _) => sv(d.getEntry(i,0))}))
      cfor(0)(_ < d.getRowDimension, _ + 1) { j: Int =>
        if (d.getEntry(j, 0) == 0)
          covVec.setEntry(j, 0, covVec.getEntry(j, 0) + nugget)
      }
      val mu: Double = points.foldLeft(0.0)(_ + _.data) / n
      val kTemp: RealMatrix = covVec.transpose().multiply(CC)
      val kPredict = mu + kTemp.multiply(VMatrix.getSubMatrix(distanceID, Array(0)).subtract(UCol.getSubMatrix(distanceID, Array(0)).scalarMultiply(mu))).getEntry(0, 0)
      val kVar = math.sqrt(sill - kTemp.multiply(covVec).getEntry(0, 0))
      prediction(i) = (kPredict, kVar)
    }
    prediction
  }
}

/**
 * @param points          Sample points for training
 * @param bandwidth       The maximum inter-point pair-distances which influence the prediction
 * @param sv              [[Semivariogram]] to be used for Kriging prediction
 */
class KrigingOrdinary(points: Seq[PointFeature[Double]], bandwidth: Double, sv: Semivariogram) extends KrigingVectorInterpolationMethod {

  /**
   * Ordinary Kriging Prediction for a single point
   */
  def createPredictor(): Point => (Double, Double) = {
    P: Point => predict(Array(P))(0)
  }

  /**
   * Ordinary Kriging Prediction for an Array of points
   * @param pointMatrix Points to be Krigred
   * @return            Tuples of (krigedValues, krigedVariance) for each of the kriged points
   */
  def predict(pointMatrix: Array[Point]): Array[(Double, Double)] = {
    val n: Int = points.length
    val colUnit: RealMatrix = MatrixUtils.createColumnRealMatrix(Array.fill(n)(1))
    val prediction: Array[(Double, Double)] = Array.ofDim[(Double, Double)](pointMatrix.length)
    val VMatrix: RealMatrix = MatrixUtils.createColumnRealMatrix(points.map(x => x.data).toArray)
    val XY: RealMatrix = MatrixUtils.createRealMatrix(Array.tabulate(points.length, 2) {
      (i, j) => {
        if (j == 0) points(i).geom.x
        else points(i).geom.y
      }
    })
    val distances: RealMatrix = distanceMatrix(XY)
    val (range: Double, sill: Double, nugget: Double) = (sv.range, sv.sill, sv.nugget)
    //Covariogram Matrix
    var C: RealMatrix = colUnit.multiply(colUnit.transpose()).scalarMultiply(sill).subtract(varianceMatrixGen(sv, distances)).add(MatrixUtils.createRealIdentityMatrix(n).scalarMultiply(nugget))
    val rank: Int = new SingularValueDecomposition(C).getRank
    if (rank < C.getRowDimension)
      C = C.add(MatrixUtils.createRealIdentityMatrix(n).scalarMultiply(0.0000001))
    val muTemp: RealMatrix = colUnit.transpose().multiply(new EigenDecomposition(C).getSolver.getInverse)
    val mu: Double = muTemp.multiply(VMatrix).getEntry(0, 0) / muTemp.multiply(colUnit).getEntry(0, 0)
    val Residual: RealMatrix = VMatrix.subtract(colUnit.scalarMultiply(mu))
    cfor(0)(_ < pointMatrix.length, _ + 1) { i: Int =>
      val pointPredict: Point = pointMatrix(i)
      val distanceSeq: Array[(PointFeature[Double], Int)] = getDistances(points, pointPredict)
      val distanceID: Array[Int] = getPointDistances(points, distanceSeq, 3, bandwidth, pointPredict)
      val distanceFromSample: RealMatrix = MatrixUtils.createColumnRealMatrix(Array.tabulate(n) { i => distanceSeq(i)._1.data })
      //Local Covariogrances
      val CC: RealMatrix = new EigenDecomposition(C.getSubMatrix(distanceID, distanceID)).getSolver.getInverse
      val d: RealMatrix = distanceFromSample.getSubMatrix(distanceID, Array(0))
      //Local Covariance Vector
      val covVec: RealMatrix = colUnit.getSubMatrix(distanceID, Array(0)).scalarMultiply(sill).subtract(MatrixUtils.createRealMatrix(Array.tabulate(d.getRowDimension, 1){(i, _) => sv(d.getEntry(i,0))}))
      cfor(0)(_ < d.getRowDimension, _ + 1) { j: Int =>
        if (d.getEntry(j, 0) == 0)
          covVec.setEntry(j, 0, covVec.getEntry(j, 0) + nugget)
      }
      val Z: RealMatrix = MatrixUtils.createColumnRealMatrix(Array.fill(d.getRowDimension)(1))
      val scalarDenom: Double = Z.transpose().multiply(CC).multiply(Z).getEntry(0,0)
      val scalarNum: Double = 1 - Z.transpose().multiply(CC).multiply(covVec).getEntry(0,0)
      val kPredict = mu + covVec.transpose().scalarAdd(scalarNum/scalarDenom).multiply(CC.multiply(Residual.getSubMatrix(distanceID, Array(0)))).getEntry(0,0)
      val kVar = math.sqrt(sill - covVec.transpose().multiply(CC).multiply(covVec).getEntry(0,0) + math.pow(scalarNum,2)/scalarDenom)
      prediction(i) = (kPredict, kVar)
    }
    prediction
  }
}

/**
 * @param points          Sample points for training
 * @param attributeSample Sample points' attribute matrix (which decides how the point coordinates guide the pointData's value)
 * @param attribute       Prediction points' attribute matrix
 * @param bandwidth       The maximum inter-point pair-distances which influence the prediction
 * @param model           The [[ModelType]] to be used for prediction
 */
class KrigingUniversal(points: Array[PointFeature[Double]], attributeSample: Array[Array[Double]], attribute: Array[Array[Double]], bandwidth: Double, model: ModelType) extends KrigingVectorInterpolationMethod {

  /**
   * Universal Kriging Prediction for a single point
   */
  def createPredictor(): Point => (Double, Double) = {
    P: Point => predict(Array(P))(0)
  }

  /**
   * Universal Kriging Prediction for an Array of points
   * @param pointMatrix Points to be Krigred
   * @return            Tuples of (krigedValues, krigedVariance) for each of the kriged points
   */
  def predict(pointMatrix: Array[Point]): Array[(Double, Double)] = {
    val n: Int = points.length
    if (n == 0)
      throw new IllegalArgumentException("No points in the training dataset")

    val k: Int = attributeSample(0).length
    val UCol: RealMatrix = MatrixUtils.createColumnRealMatrix(Array.fill(n)(1))
    val attrSample = Array.ofDim[Double](n, k+1)
    cfor(0)(_ < n, _ + 1) { row =>
      attrSample(row) = Array(1.0) ++ attributeSample(row)
    }
    val XX: RealMatrix = MatrixUtils.createRealMatrix(attrSample)
    val S: RealMatrix = new EigenDecomposition(MatrixUtils.createRealDiagonalMatrix(Array.tabulate(k+1){i => absArray(XX.getColumn(i)).max})).getSolver.getInverse
    val Z: RealMatrix = XX.multiply(S)
    val MTemp: RealMatrix = MatrixUtils.createRealMatrix(Array.tabulate(points.length, 2) {
      (i, j) => {
        if (j == 0) points(i).geom.x
        else points(i).geom.y
      }
    })
    val y0: RealMatrix = MatrixUtils.createColumnRealMatrix(points.map(x => x.data))
    var bb: RealMatrix = new SingularValueDecomposition(Z).getSolver.solve(y0)
    var b: RealMatrix = S.multiply(bb)
    val e: RealMatrix = y0.subtract(XX.multiply(b))
    val pointsFitting: Array[PointFeature[Double]] = Array.tabulate(n) {row: Int => PointFeature(points(row).geom, e.getEntry(row, 0)) }
    val res: Semivariogram = NonLinearSemivariogram(pointsFitting, 0, 0, model)

    //Covariogram Matrix
    var C: RealMatrix = UCol.multiply(UCol.transpose()).scalarMultiply(res.sill).subtract(varianceMatrixGen(res, distanceMatrix(MTemp))).add(MatrixUtils.createRealIdentityMatrix(n).scalarMultiply(res.nugget))
    val rank: Int = new SingularValueDecomposition(C).getRank
    if (rank < C.getRowDimension)
      C = C.add(MatrixUtils.createRealIdentityMatrix(n).scalarMultiply(0.0001))
    val AA: RealMatrix = new LUDecomposition(new CholeskyDecomposition(C).getL).getSolver.getInverse
    bb = new SingularValueDecomposition(AA.multiply(Z)).getSolver.solve(AA.multiply(y0))
    b = S.multiply(bb)
    val N: Int = pointMatrix.length
    val XXArrayNew = Array.ofDim[Double](N, k+1)
    cfor(0)(_ < N, _ + 1) { row =>
      XXArrayNew(row) = Array(1.0) ++ attribute(row)
    }
    val XXNew: RealMatrix = MatrixUtils.createRealMatrix(XXArrayNew)
    val prediction: Array[(Double, Double)] = Array.ofDim[(Double, Double)](pointMatrix.length)
    val Residual: RealMatrix = y0.subtract(XX.multiply(b))
    cfor(0)(_ < N, _ + 1) { i: Int =>
      val distanceSeq: Array[(PointFeature[Double], Int)] = getDistances(points, pointMatrix(i))
      val distanceID: Array[Int] = getPointDistances(points, distanceSeq, k+2, bandwidth, pointMatrix(i))
      val CC: RealMatrix = new SingularValueDecomposition(C.getSubMatrix(distanceID, distanceID)).getSolver.getInverse
      val d: RealMatrix = MatrixUtils.createColumnRealMatrix(Array.tabulate(n) { i => distanceSeq(i)._1.data }).getSubMatrix(distanceID, Array(0))
      val cSmall: RealMatrix = UCol.getSubMatrix(distanceID, Array(0)).scalarMultiply(res.sill).subtract(MatrixUtils.createRealMatrix(Array.tabulate(d.getRowDimension, 1){(i, _) => res(d.getEntry(i,0))}))
      cfor(0)(_ < d.getRowDimension, _ + 1) { j: Int =>
        if (d.getEntry(j, 0) == 0)
          cSmall.setEntry(j, 0, cSmall.getEntry(j, 0) + res.nugget)
      }
      val kPredict: Double = MatrixUtils.createRowRealMatrix(XXNew.getRow(i)).multiply(b).getEntry(0,0) + cSmall.transpose().multiply(CC).multiply(Residual.getSubMatrix(distanceID,Array(0))).getEntry(0,0)
      val ZZ: RealMatrix = Z.getSubMatrix(distanceID, Array.tabulate(XX.getColumnDimension){i => i})
      val rankTemp: Int = new SingularValueDecomposition(ZZ.transpose().multiply(CC).multiply(ZZ)).getRank
      val B: RealMatrix =
        if (rankTemp < k + 1)
          S.multiply(new SingularValueDecomposition(ZZ.transpose().multiply(CC).multiply(ZZ)).getSolver.getInverse).multiply(S)
        else
          S.multiply(new EigenDecomposition(ZZ.transpose().multiply(CC).multiply(ZZ)).getSolver.getInverse).multiply(S)
      val kVarTemp: RealMatrix = MatrixUtils.createColumnRealMatrix(XXNew.getRow(i)).subtract(XX.getSubMatrix(distanceID, Array.tabulate(XX.getColumnDimension){i => i}).transpose().multiply(CC).multiply(cSmall))
      val kVar: Double = math.sqrt(res.sill - cSmall.transpose().multiply(CC).multiply(cSmall).getEntry(0,0) + kVarTemp.transpose().multiply(B).multiply(kVarTemp).getEntry(0,0))
      prediction(i) = (kPredict, kVar)
    }
    prediction
  }
}

/**
 * @param points          Sample points for training
 * @param attributeSample Sample points' attribute matrix (which decides how the point coordinates guide the pointData's value)
 * @param attribute       Prediction points' attribute matrix
 * @param bandwidth       The maximum inter-point pair-distances which influence the prediction
 * @param model           The [[ModelType]] to be used for prediction
 */
class KrigingGeo(points: Array[PointFeature[Double]], attributeSample: Array[Array[Double]], attribute: Array[Array[Double]], bandwidth: Double, model: ModelType) extends KrigingVectorInterpolationMethod {

  /**
   * Geostatistical Kriging Prediction for a single point
   */
  def createPredictor(): Point => (Double, Double) = {
    P: Point => predict(Array(P))(0)
  }

  /**
   * Geostatistical Kriging Prediction for an Array of points
   * @param pointMatrix Points to be Krigred
   * @return            Tuples of (krigedValues, krigedVariance) for each of the kriged points
   */
  def predict(pointMatrix: Array[Point]): Array[(Double, Double)] = {
    val n: Int = points.length
    if (n == 0)
      throw new IllegalArgumentException("No points in the training dataset")

    val k: Int = attributeSample(0).length
    val UCol: RealMatrix = MatrixUtils.createColumnRealMatrix(Array.fill(n)(1))
    val attrSample = Array.ofDim[Double](n, k+1)
    cfor(0)(_ < n, _ + 1) { row =>
      attrSample(row) = Array(1.0) ++ attributeSample(row)
    }
    val XX: RealMatrix = MatrixUtils.createRealMatrix(attrSample)
    val S: RealMatrix = new EigenDecomposition(MatrixUtils.createRealDiagonalMatrix(Array.tabulate(k+1){i => absArray(XX.getColumn(i)).max})).getSolver.getInverse
    val Z: RealMatrix = XX.multiply(S)
    val MTemp: RealMatrix = MatrixUtils.createRealMatrix(Array.tabulate(points.length, 2) {
      (i, j) => {
        if (j == 0) points(i).geom.x
        else points(i).geom.y
      }
    })
    val D: RealMatrix = distanceMatrix(MTemp)
    val y0: RealMatrix = MatrixUtils.createColumnRealMatrix(points.map(x => x.data))
    var bb: RealMatrix = new SingularValueDecomposition(Z).getSolver.solve(y0)
    var b: RealMatrix = S.multiply(bb)
    var e: RealMatrix = y0.subtract(XX.multiply(b))
    var pointsFitting: Array[PointFeature[Double]] = Array.tabulate(n) {row: Int => PointFeature(points(row).geom, e.getEntry(row, 0)) }
    var res: Semivariogram = NonLinearSemivariogram(pointsFitting, 0, 0, model)
    var delta: Double = 1.0
    var A: RealMatrix = UCol
    var AA: RealMatrix = UCol
    println("Delta = " + delta)
    while (delta > 0.001) {
      val b0: RealMatrix = b
      var C: RealMatrix = UCol.multiply(UCol.transpose()).scalarMultiply(res.sill).subtract(varianceMatrixGen(res, D)).add(MatrixUtils.createRealIdentityMatrix(n).scalarMultiply(res.nugget))
      val rank: Int = new SingularValueDecomposition(C).getRank
      if (rank < C.getRowDimension)
        C = C.add(MatrixUtils.createRealIdentityMatrix(n).scalarMultiply(0.0001))
      A = new CholeskyDecomposition(C).getL
      AA = new LUDecomposition(A).getSolver.getInverse
      bb = new SingularValueDecomposition(AA.multiply(Z)).getSolver.solve(AA.multiply(y0))
      b = S.multiply(bb)
      e = y0.subtract(XX.multiply(b))
      pointsFitting = Array.tabulate(n) {row: Int => PointFeature(points(row).geom, e.getEntry(row, 0)) }
      val process: Array[Double] = b.subtract(b0).getColumn(0)
      delta = absArray(Array.tabulate(process.length){i => math.abs(process(i))/b0.getEntry(i,0)}).max
      println("Delta = " + delta)
      if(delta > 0.001)
        res = NonLinearSemivariogram(pointsFitting, 0, 0, model)
    }
    val C: RealMatrix = UCol.multiply(UCol.transpose()).scalarMultiply(res.sill).subtract(varianceMatrixGen(res, D)).add(MatrixUtils.createRealIdentityMatrix(n).scalarMultiply(res.nugget))
    val cov_bb: RealMatrix = new LUDecomposition(Z.transpose().multiply(AA.transpose().multiply(AA).multiply(Z))).getSolver.getInverse
    val cov_b: RealMatrix = S.multiply(cov_bb.multiply(S.transpose()))
    e = y0.subtract(XX.multiply(b))
    val N: Int = pointMatrix.length
    val predictionAttrArray = Array.ofDim[Double](N, k+1)
    cfor(0)(_ < N, _ + 1) { row =>
      predictionAttrArray(row) = Array(1.0) ++ attribute(row)
    }
    val predictionAttr: RealMatrix = MatrixUtils.createRealMatrix(predictionAttrArray)
    val prediction: Array[(Double, Double)] = Array.ofDim[(Double, Double)](pointMatrix.length)
    val Residual: RealMatrix = y0.subtract(XX.multiply(b))
    cfor(0)(_ < N, _ + 1) { i: Int =>
      val pointPredict: Point = pointMatrix(i)
      val distanceSeq: Array[(PointFeature[Double], Int)] = getDistances(points, pointPredict)
      val distanceID: Array[Int] = getPointDistances(points, distanceSeq, k+2, bandwidth, pointPredict)
      val distanceFromSample: RealMatrix = MatrixUtils.createColumnRealMatrix(Array.tabulate(n) { i => distanceSeq(i)._1.data })
      val CC: RealMatrix = new SingularValueDecomposition(C.getSubMatrix(distanceID, distanceID)).getSolver.getInverse
      val d: RealMatrix = distanceFromSample.getSubMatrix(distanceID, Array(0))
      val c: RealMatrix = UCol.getSubMatrix(distanceID, Array(0)).scalarMultiply(res.sill).subtract(MatrixUtils.createRealMatrix(Array.tabulate(d.getRowDimension, 1){(i, _) => res(d.getEntry(i,0))}))
      cfor(0)(_ < d.getRowDimension, _ + 1) { j: Int =>
        if (d.getEntry(j, 0) == 0)
          c.setEntry(j, 0, c.getEntry(j, 0) + res.nugget)
      }
      val kPredict: Double = MatrixUtils.createRowRealMatrix(predictionAttr.getRow(i)).multiply(b).getEntry(0,0) + c.transpose().multiply(CC).multiply(Residual.getSubMatrix(distanceID,Array(0))).getEntry(0,0)
      val wSmall: RealMatrix = MatrixUtils.createColumnRealMatrix(predictionAttr.getRow(i))
      val W: RealMatrix = XX.getSubMatrix(distanceID, Array.tabulate(XX.getColumnDimension){i => i})
      val ZZ: RealMatrix = Z.getSubMatrix(distanceID, Array.tabulate(XX.getColumnDimension){i => i})
      val rankTemp: Int = new SingularValueDecomposition(ZZ.transpose().multiply(CC).multiply(ZZ)).getRank
      val B: RealMatrix =
        if (rankTemp < k + 1)
          S.multiply(new SingularValueDecomposition(ZZ.transpose().multiply(CC).multiply(ZZ)).getSolver.getInverse).multiply(S)
        else
          S.multiply(new EigenDecomposition(ZZ.transpose().multiply(CC).multiply(ZZ)).getSolver.getInverse).multiply(S)
      val kVarTemp: RealMatrix = wSmall.subtract(W.transpose().multiply(CC).multiply(c))
      val kVar: Double = math.sqrt(res.sill - c.transpose().multiply(CC).multiply(c).getEntry(0,0) + kVarTemp.transpose().multiply(B).multiply(kVarTemp).getEntry(0,0))
      prediction(i) = (kPredict, kVar)
    }
    prediction
  }
}
