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

trait KrigingVectorInterpolationMethod{
  def createPredictor(): Point => (Double, Double)
  def predict(pointMatrix: Array[Point]): Array[(Double, Double)]

  def distance(p1: Point, p2: Point) = math.abs(math.sqrt(math.pow(p1.x - p2.x, 2) + math.pow(p1.y - p2.y, 2)))
  def varianceMatrixGen(range: Double, sill: Double, nugget: Double, sv: Double => Double, distanceM: RealMatrix): RealMatrix = {
    val n: Int = distanceM.getRowDimension
    val varMatrix: RealMatrix = MatrixUtils.createRealMatrix(n, n)

    //Variance shifts to sill, if distance>range
    cfor(0)(_ < n, _ + 1) { i =>
      cfor(0)(_ < n, _ + 1) { j =>
        distanceM.setEntry(i,j,math.min(distanceM.getEntry(i,j), range))
        varMatrix.setEntry(i,j,sv(distanceM.getEntry(i,j)))
      }
    }
    varMatrix
  }

  def getDistances(points: Seq[PointFeature[Double]], point: Point): Array[(PointFeature[Double], Int)] = {
    //Returns a cluster of distances of pointPredict from the Sample Point Set along with the indices
    var distanceID: Array[(PointFeature[Double], Int)] = Array()
    cfor(0)(_ < points.length, _ + 1) { j: Int =>
      distanceID = distanceID :+(PointFeature(points(j).geom, distance(points(j).geom, point)), j)
    }
    distanceID
  }
  def getPointDistances(points: Seq[PointFeature[Double]], distanceID: Array[(PointFeature[Double], Int)], bandwidth: Double, point: Point): Array[Int] = {
    //Returns the indices of points close to the point for prediction within the given bandwidth
    //In case the number of points<3; it returns the closest three points
    var sequenceID: Array[Int] = Array()
    cfor(0)(_ < points.length, _ + 1) { j: Int =>
      val curDist = distance(points(j).geom, point)
      if (curDist < bandwidth)
        sequenceID = sequenceID :+ j
    }
    if (sequenceID.length < 3) {
      var sequence3ID: Array[Int] = Array()
      distanceID.sortWith((f, s) => f._1.data < s._1.data)
      sequence3ID = sequence3ID :+ distanceID(0)._2 :+ distanceID(1)._2 :+ distanceID(2)._2
      sequence3ID
    }
    else
      sequenceID
  }

  def distanceMatrix(xy: RealMatrix): RealMatrix = {
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

class KrigingSimple(points: Seq[PointFeature[Double]], bandwidth: Double, svParam: Array[Double], model: ModelType) extends KrigingVectorInterpolationMethod {
  val sv: Double => Double = NonLinearSemivariogram.explicitModel(svParam, model)
  def createPredictor(): Point => (Double, Double) = {
    P: Point => predict(Array(P))(0)
  }
  def predict(pointMatrix: Array[Point]): Array[(Double, Double)] = {
    val n: Int = points.length
    val UCol: RealMatrix = MatrixUtils.createColumnRealMatrix(Array.fill(n)(1))
    val prediction: Array[(Double, Double)] = Array.ofDim[(Double, Double)](pointMatrix.length)
    val VMatrix: RealMatrix = MatrixUtils.createColumnRealMatrix(points.map(x => x.data).toArray)
    val XY: RealMatrix = MatrixUtils.createRealMatrix(Array.tabulate(points.length, 2) {
      (i, j) => {
        if (j == 0) points(i).geom.x
        else points(i).geom.y
      }
    })
    val distances: RealMatrix = distanceMatrix(XY)
    val (range: Double, sill: Double, nugget: Double) = (svParam(0) ,svParam(1) ,svParam(2))
    //Covariogram Matrix
    val C: RealMatrix = UCol.multiply(UCol.transpose()).scalarMultiply(sill).subtract(varianceMatrixGen(range, sill, nugget, sv, distances)).add(MatrixUtils.createRealIdentityMatrix(n).scalarMultiply(nugget))
    cfor(0)(_ < pointMatrix.length, _ + 1) { i: Int =>
      val pointPredict: Point = pointMatrix(i)
      val distanceSeq: Array[(PointFeature[Double], Int)] = getDistances(points, pointPredict)
      val distanceID: Array[Int] = getPointDistances(points, distanceSeq, bandwidth, pointPredict)
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
      var mu: Double = points.foldLeft(0.0)(_ + _.data) / n
      val kTemp: RealMatrix = covVec.transpose().multiply(CC)
      val kPredict = mu + kTemp.multiply(VMatrix.getSubMatrix(distanceID, Array(0)).subtract(UCol.getSubMatrix(distanceID, Array(0)).scalarMultiply(mu))).getEntry(0, 0)
      val kVar = math.sqrt(sill - kTemp.multiply(covVec).getEntry(0, 0))
      prediction(i) = (kPredict, kVar)
    }
    prediction
  }
}

class KrigingOrdinary(points: Seq[PointFeature[Double]], bandwidth: Double, svParam: Array[Double], model: ModelType) extends KrigingVectorInterpolationMethod {
  val sv: Double => Double = NonLinearSemivariogram.explicitModel(svParam, model)
  def createPredictor(): Point => (Double, Double) = {
    P: Point => predict(Array(P))(0)
  }
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
    val (range: Double, sill: Double, nugget: Double) = (svParam(0) ,svParam(1) ,svParam(2))
    //Covariogram Matrix
    var C: RealMatrix = colUnit.multiply(colUnit.transpose()).scalarMultiply(sill).subtract(varianceMatrixGen(range, sill, nugget, sv, distances)).add(MatrixUtils.createRealIdentityMatrix(n).scalarMultiply(nugget))
    val rank: Int = new SingularValueDecomposition(C).getRank
    if (rank < C.getRowDimension)
      C = C.add(MatrixUtils.createRealIdentityMatrix(n).scalarMultiply(0.0000001))
    val muTemp: RealMatrix = colUnit.transpose().multiply(new EigenDecomposition(C).getSolver.getInverse)
    var mu: Double = muTemp.multiply(VMatrix).getEntry(0, 0) / muTemp.multiply(colUnit).getEntry(0, 0)
    val Residual: RealMatrix = VMatrix.subtract(colUnit.scalarMultiply(mu))
    cfor(0)(_ < pointMatrix.length, _ + 1) { i: Int =>
      val pointPredict: Point = pointMatrix(i)
      val distanceSeq: Array[(PointFeature[Double], Int)] = getDistances(points, pointPredict)
      val distanceID: Array[Int] = getPointDistances(points, distanceSeq, bandwidth, pointPredict)
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

class KrigingUniversal(points: Array[PointFeature[Double]], radius: Option[Double], chunkSize: Double, lag: Double = 0, model: ModelType) extends KrigingVectorInterpolationMethod {
  def getCovariogramMatrix(sv: Semivariogram, sill: Double, points: Seq[PointFeature[Double]]): RealMatrix = {
    val pointSize = points.size
    val nugget = sv(0)
    val covariogram = Array.ofDim[Double](pointSize, pointSize)
    cfor(0)(_ < pointSize, _ + 1) { row =>
      //covariogram(row)(row) = sill - nugget
      covariogram(row)(row) = sill
      cfor(0)(_ < row, _ + 1) { col =>
        covariogram(row)(col) = sill - sv(distance(points(row).geom, points(col).geom))
        covariogram(col)(row) = covariogram(row)(col)
      }
    }
    MatrixUtils.createRealMatrix(covariogram)
  }
  def createPredictor(): Point => (Double, Double) = {
    pointPredict => {
      val pointSize = points.size
      if (pointSize == 0)
        throw new IllegalArgumentException("No Points in the observation sequence")

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
      val sv: Semivariogram = NonLinearSemivariogram(points, 1.0, lag.toInt, model)

      //Full covariogram
      val sill: Double = Semivariogram.s
      val covariogram: RealMatrix = getCovariogramMatrix(sv, sill, points)
      val covarianceInverse: RealMatrix = new LUDecomposition(covariogram).getSolver.getInverse

      //GLS Estimation (Full matrix)
      val betaN: RealMatrix = new LUDecomposition(attrMatrix.transpose().multiply(covarianceInverse).multiply(attrMatrix)).getSolver.getInverse.multiply(attrMatrix.transpose()).multiply(covarianceInverse).multiply(yMatrix)

      //Prediction Set
      //TODO : Check if n >= k + 2 holds else reiterate the set generation process
      val predictionSet: Seq[PointFeature[Double]] = points.filter(x => distance(x.geom, pointPredict) < radius.get)

      val covariogramSample: RealMatrix = getCovariogramMatrix(sv, sill, predictionSet)
      val covariogramSampleInverse: RealMatrix = new LUDecomposition(covariogramSample).getSolver.getInverse
      val sillSample: Double = Semivariogram.s
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
  def predict(rasterData: Array[Point]): Array[(Double, Double)] = {
    Array((1.0, 1.0))
  }
}

class KrigingGeo(points: Seq[PointFeature[Double]], radius: Option[Double], chunkSize: Double, lag: Double = 0, model: ModelType) extends KrigingVectorInterpolationMethod {
  def getCovariogramMatrix(sv: Semivariogram, sill: Double, points: Seq[PointFeature[Double]]): RealMatrix = {
    val pointSize = points.size
    val nugget = sv(0)
    val covariogram = Array.ofDim[Double](pointSize, pointSize)
    cfor(0)(_ < pointSize, _ + 1) { row =>
      //covariogram(row)(row) = sill - nugget
      covariogram(row)(row) = sill
      cfor(0)(_ < row, _ + 1) { col =>
        covariogram(row)(col) = sill - sv(distance(points(row).geom, points(col).geom))
        covariogram(col)(row) = covariogram(row)(col)
      }
    }
    MatrixUtils.createRealMatrix(covariogram)
  }
  def createPredictor(): Point => (Double, Double) = {
    pointPredict => {
      val pointSize = points.size
      if (pointSize == 0)
        throw new IllegalArgumentException("No Points in the observation sequence")

      val attrArray = Array.ofDim[Double](pointSize, 6)
      cfor(0)(_ < pointSize, _ + 1) { row =>
        val s1 = points(row).geom.x
        val s2 = points(row).geom.y
        attrArray(row) = Array(1, s1, s2, s1 * s1, s1 * s2, s2 * s2)
      }
      val yMatrix: RealMatrix = MatrixUtils.createColumnRealMatrix(points.map(x => x.data.toDouble).toArray)
      val attrMatrix: RealMatrix = MatrixUtils.createRealMatrix(attrArray)
      var convergence: Double = 1

      //1. OLS Estimate (Beta)
      //Solving :     X * betaOLS = y
      var beta: RealMatrix = new LUDecomposition(attrMatrix.transpose().multiply(attrMatrix)).getSolver.getInverse.multiply(attrMatrix.transpose()).multiply(yMatrix)
      var error = yMatrix.subtract(attrMatrix.multiply(beta))

      while (convergence > 0.001)
      {
        val betaOld: RealMatrix = beta
        val errorOld: RealMatrix = error
        val pointsNew: Array[PointFeature[Double]] =
          Array.tabulate(pointSize){row => PointFeature(points(row).geom, errorOld.getEntry(row, 0)) }
        val abc = EmpiricalVariogram.nonlinear(pointsNew, 1.0, lag.toInt)
        val empiricalSemivariogram: Array[(Double, Double)] = Array.tabulate(abc.distances.length){i => (abc.distances(i), abc.variance(i))}
        val sv: Semivariogram = Semivariogram.fit(empiricalSemivariogram, model)
        val sill: Double = Semivariogram.s
        val covariogram: RealMatrix = getCovariogramMatrix(sv, sill, points)
        val covarianceInverse: RealMatrix = new LUDecomposition(covariogram).getSolver.getInverse
        beta = new LUDecomposition(attrMatrix.transpose().multiply(covarianceInverse).multiply(attrMatrix)).getSolver.getInverse.multiply(attrMatrix.transpose()).multiply(covarianceInverse).multiply(yMatrix)
        error = yMatrix.subtract(attrMatrix.multiply(beta))
        var Delta: Double = 0
        cfor(0)(_ < 6, _ + 1) { row =>
          Delta = math.max(Delta, math.abs((beta.getEntry(row, 0) - betaOld.getEntry(row, 0)) / betaOld.getEntry(row, 0)))
        }
        convergence = Delta
      }

      val pointsNew: Array[PointFeature[Double]] =
        Array.tabulate(pointSize) {row: Int => PointFeature(points(row).geom, error.getEntry(row, 0)) }
      val abc = EmpiricalVariogram.nonlinear(pointsNew, 1.0, lag.toInt)
      val empiricalSemivariogram: Array[(Double, Double)] = Array.tabulate(abc.distances.length){i => (abc.distances(i), abc.variance(i))}
      val sv: Semivariogram = Semivariogram.fit(empiricalSemivariogram, model)
      val sill: Double = Semivariogram.s
      val covariogram: RealMatrix = getCovariogramMatrix(sv, sill, points)
      val covarianceInverse: RealMatrix = new LUDecomposition(covariogram).getSolver.getInverse

      val betaN: RealMatrix = new LUDecomposition(attrMatrix.transpose().multiply(covarianceInverse).multiply(attrMatrix)).getSolver.getInverse.multiply(attrMatrix.transpose()).multiply(covarianceInverse).multiply(yMatrix)
      val predictionSet: Seq[PointFeature[Double]] = points.filter(x => distance(x.geom, pointPredict) < radius.get.toDouble)
      val covariogramSample: RealMatrix = getCovariogramMatrix(sv, sill, predictionSet)
      val covariogramSampleInverse: RealMatrix = new LUDecomposition(covariogramSample).getSolver.getInverse
      val sillSample: Double = Semivariogram.s
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
      val part2_1: RealMatrix = x0.transpose().subtract(attrSampleMatrix.transpose().multiply(covariogramSampleInverse).multiply(cSampleMatrix.transpose())).transpose()
      var kTemp = attrSampleMatrix.transpose().multiply(covariogramSampleInverse).multiply(attrSampleMatrix).add(MatrixUtils.createRealIdentityMatrix(6))
      val part2_2: RealMatrix = new LUDecomposition(kTemp).getSolver.getInverse
      val part2_3: RealMatrix = x0.transpose().subtract(attrSampleMatrix.transpose().multiply(covariogramSampleInverse).multiply(cSampleMatrix.transpose()))

      val krigingVariance: Double = math.sqrt(part1 + part2_1.multiply(part2_2).multiply(part2_3).getEntry(0, 0))
      (prediction, krigingVariance)
    }
  }
  def predict(rasterData: Array[Point]): Array[(Double, Double)] = {
    Array((1.0, 1.0))
  }
}
