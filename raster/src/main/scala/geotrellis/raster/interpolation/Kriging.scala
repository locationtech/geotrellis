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

import geotrellis.raster.{DoubleArrayTile, RasterExtent, Tile}
import geotrellis.vector.{PointFeature, Extent}
import geotrellis.vector.Point
import geotrellis.vector.interpolation.Semivariogram

import org.apache.commons.math3.linear.{EigenDecomposition, MatrixUtils, RealMatrix}
import org.apache.commons.math3.linear.{MatrixUtils, RealMatrix}

import spire.syntax.cfor._

import scala.collection.mutable

trait Kriging{
  //def createPredictor(): Point => (Double, Double)
  //def createPredictor(numberOfPoints: Int): Point => (Double, Double)

  //def createPredictor(numberOfPoints: Int): (Double, Double) => (Double, Double)
  //def predict(pointMatrix: Array[Point]): Array[(Double, Double)]

  private def distance(p1: Point, p2: Point) = math.abs(math.sqrt(math.pow(p1.x - p2.x, 2) + math.pow(p1.y - p2.y, 2)))

  protected def varianceMatrixGen(sv: Semivariogram, points: Array[PointFeature[Double]]): RealMatrix = {
    val n = points.length
    val varianceMatrix: RealMatrix = MatrixUtils.createRealMatrix(n, n)
    cfor(0)(_ < n, _ + 1) { i: Int =>
      varianceMatrix.setEntry(i, i, sv.nugget)
      cfor(i + 1)(_ < n, _ + 1) { j: Int =>
        val dx = points(i).geom.x - points(j).geom.x
        val dy = points(i).geom.y - points(j).geom.y
        val varVal: Double = sv(math.min(math.sqrt(dx * dx + dy * dy), sv.range))
        varianceMatrix.setEntry(i, j, varVal)
        varianceMatrix.setEntry(j, i, varVal)
      }
    }
    varianceMatrix
  }

  /**
   * Returns the indices of points close to the point for prediction within the given bandwidth
   * In case the number of points < minPoints; it returns the closest minPoints number of points
   */
  protected def getPointDistancesSorted(points: Array[PointFeature[Double]], minPoints: Int, bandwidth: Double, point: Point): Array[(Int, Double)] = {
    val distances =
      new mutable.PriorityQueue[(Int, Double)]()(Ordering.by(-1 * _._2))

    cfor(0)(_ < points.length, _ + 1) { i =>
      val dVal: Double = distance(points(i), point)
      distances += ((i, dVal))
    }

    val q = distances.dequeueAll
    val result = q.takeWhile(_._2 <= bandwidth).toArray
    if(result.length < minPoints)
      q.take(minPoints).toArray
    else
      result
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
}

class SimpleKriging(points: Array[PointFeature[Double]], bandwidth: Double, sv: Semivariogram) extends Kriging {

  /**
   * Simple Kriging Prediction for a single point
   */
  private def createPredictor(numberOfPoints: Int): (Double, Double) => (Double, Double) = {
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
    //val krigingPrediction = Array.ofDim[(Double, Double)](numberOfPoints)

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

  def predict(tile: Tile, extent: Extent): Tile = {
    val numberOfCells = tile.cols * tile.rows
    val rasterExtent = RasterExtent(extent, tile.cols, tile.rows)
    //Pay heed to this aspect
    val unitCol: RealMatrix = MatrixUtils.createColumnRealMatrix(Array.fill(numberOfCells)(1))

    val krigingPrediction = DoubleArrayTile.empty(tile.cols, tile.rows)
    val predictor = createPredictor(tile.cols * tile.rows)

    cfor(0)(_ < tile.cols, _ + 1) { col =>
      cfor(0)(_ < tile.rows, _ + 1) { row =>
        val (x, y) = rasterExtent.gridToMap(col, row)
        val (kPredict, _) = predictor(x, y)

        krigingPrediction.setDouble(col, row, kPredict)
      }
    }

    krigingPrediction
  }

  def predict(pointMatrix: Array[Point]): Array[(Double, Double)] = {
    val n: Int = points.length
    if (n == 0)
      throw new IllegalArgumentException("No points in the training dataset")

    val krigingPrediction: Array[(Double, Double)] = Array.ofDim[(Double, Double)](pointMatrix.length)
    //val predictor = createPredictor(n)
    val predictor = createPredictor(pointMatrix.length)

    cfor(0)(_ < pointMatrix.length, _ + 1) { i: Int =>
      val pointPredict: Point = pointMatrix(i)
      krigingPrediction(i) = predictor(pointPredict.x, pointPredict.y)
    }

    krigingPrediction
  }

  /*
  def predict(tile: Tile, extent: Extent): Tile = {
    val numberOfCells = tile.cols * tile.rows
    val rasterExtent = RasterExtent(extent, tile.cols, tile.rows)
    val unitCol: RealMatrix = MatrixUtils.createColumnRealMatrix(Array.fill(numberOfCells)(1))

    //val krigingPrediction: MutableArrayTile = DoubleArrayTile.empty(tile.cols, tile.rows)
    val krigingPrediction = DoubleArrayTile.empty(tile.cols, tile.rows)
    val predictor = createPredictor(tile.cols * tile.rows)

    val output1 = DoubleArrayTile.empty(tile.cols, tile.rows)
    cfor(0)(_ < tile.cols, _ + 1) { col =>
      cfor(0)(_ < tile.rows, _ + 1) { row =>
        val (x, y) = rasterExtent.gridToMap(col, row)
        val (kPredict, _) = predictor(x, y)

        krigingPrediction.setDouble(col, row, kPredict)
      }
    }

    krigingPrediction
  }

  def predict(pointMatrix: Array[Point]): Array[(Double, Double)] = {
    val n: Int = points.length
    if (n == 0)
      throw new IllegalArgumentException("No points in the training dataset")

    val krigingPrediction: Array[(Double, Double)] = Array.ofDim[(Double, Double)](pointMatrix.length)
    val predictor = createPredictor(n)

    cfor(0)(_ < pointMatrix.length, _ + 1) { i: Int =>
      val pointPredict: Point = pointMatrix(i)
      krigingPrediction(i) = predictor(pointPredict.x, pointPredict.y)
    }

    krigingPrediction
  }
   */

  /*def createPredictor(): Point => (Double, Double) = {
    P: Point => predict(Array(P))(0)
  }*/

  /**
   * Simple Kriging Prediction for an Array of points
   * @param pointMatrix Points to be Kriged
   * @return            Tuples of (krigedValues, krigedVariance) for each of the kriged points
   */
  def predict123(pointMatrix: Array[Point]): Array[(Double, Double)] = {
    val n: Int = points.length
    if (n == 0)
      throw new IllegalArgumentException("No points in the training dataset")

    val unitCol = MatrixUtils.createColumnRealMatrix(Array.fill(n)(1))
    val covariogramMatrix: RealMatrix =
      unitCol.multiply(unitCol.transpose())
        .scalarMultiply(sv.sill)
        .subtract(varianceMatrixGen(sv, points))
        .add(MatrixUtils.createRealIdentityMatrix(n).scalarMultiply(sv.nugget))

    val ptData = MatrixUtils.createColumnRealMatrix(points.map(x => x.data))

    val krigingPrediction = Array.ofDim[(Double, Double)](pointMatrix.length)
    cfor(0)(_ < pointMatrix.length, _ + 1) { i: Int =>
      val pointPredict: Point = pointMatrix(i)
      val distanceSortedInfo: Array[(Int, Double)] = getPointDistancesSorted(points, 3, bandwidth, pointPredict)
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
      krigingPrediction(i) = (kPredict, kVar)
    }
    krigingPrediction
  }
}

/*
class SimpleKriging(points: Array[PointFeature[Double]], bandwidth: Double, sv: Semivariogram) extends Kriging {

  private def createPredictor(numberOfPoints: Int): (Double, Double) => (Double, Double) = {
    val unitCol: RealMatrix = MatrixUtils.createColumnRealMatrix(Array.fill(numberOfPoints)(1))

    //Covariogram Matrix
    val covarianceMatrix: RealMatrix =
      unitCol
        .multiply(unitCol.transpose())
        .scalarMultiply(sv.sill)
        .subtract(varianceMatrixGen(sv, points))
        .add(MatrixUtils.createRealIdentityMatrix(numberOfPoints).scalarMultiply(sv.nugget))

    val ptData: RealMatrix = MatrixUtils.createColumnRealMatrix(points.map(x => x.data))

    { (x, y) =>
      val pointPredict: Point = Point(x, y)
      val distanceSortedSimple = getPointDistancesSorted(points, 3, bandwidth, pointPredict)
      val distanceID: Array[Int] = distanceSortedSimple.map(_._1)
      //Local Covariances
      val CC: RealMatrix = new EigenDecomposition(covarianceMatrix.getSubMatrix(distanceID, distanceID)).getSolver.getInverse
      val d: RealMatrix = MatrixUtils.createColumnRealMatrix(distanceSortedSimple.map(_._2))
      //Local Covariance Vector
      val covVec: RealMatrix = unitCol.getSubMatrix(distanceID, Array(0)).scalarMultiply(sv.sill).subtract(MatrixUtils.createRealMatrix(Array.tabulate(d.getRowDimension, 1){(i, _) => sv(d.getEntry(i,0))}))
      cfor(0)(_ < d.getRowDimension, _ + 1) { j: Int =>
        if (d.getEntry(j, 0) == 0)
          covVec.setEntry(j, 0, covVec.getEntry(j, 0) + sv.nugget)
      }
      val mu: Double = points.foldLeft(0.0)(_ + _.data) / numberOfPoints
      val kTemp: RealMatrix = covVec.transpose().multiply(CC)
      val kPredict = mu + kTemp.multiply(ptData.getSubMatrix(distanceID, Array(0)).subtract(unitCol.getSubMatrix(distanceID, Array(0)).scalarMultiply(mu))).getEntry(0, 0)
      val kVar = math.sqrt(sv.sill - kTemp.multiply(covVec).getEntry(0, 0))
      (kPredict, kVar)
    }
  }

  def predict(tile: Tile, extent: Extent): Tile = {
    val numberOfCells = tile.cols * tile.rows
    val rasterExtent = RasterExtent(extent, tile.cols, tile.rows)
    val unitCol: RealMatrix = MatrixUtils.createColumnRealMatrix(Array.fill(numberOfCells)(1))

    //val krigingPrediction: MutableArrayTile = DoubleArrayTile.empty(tile.cols, tile.rows)
    val krigingPrediction = DoubleArrayTile.empty(tile.cols, tile.rows)
    val predictor = createPredictor(tile.cols * tile.rows)

    val output1 = DoubleArrayTile.empty(tile.cols, tile.rows)
    cfor(0)(_ < tile.cols, _ + 1) { col =>
      cfor(0)(_ < tile.rows, _ + 1) { row =>
        val (x, y) = rasterExtent.gridToMap(col, row)
        val (kPredict, _) = predictor(x, y)

        krigingPrediction.setDouble(col, row, kPredict)
      }
    }

    krigingPrediction
  }

  def predict(pointMatrix: Array[Point]): Array[(Double, Double)] = {
    val n: Int = points.length
    if (n == 0)
      throw new IllegalArgumentException("No points in the training dataset")

    val krigingPrediction: Array[(Double, Double)] = Array.ofDim[(Double, Double)](pointMatrix.length)
    val predictor = createPredictor(n)

    cfor(0)(_ < pointMatrix.length, _ + 1) { i: Int =>
      val pointPredict: Point = pointMatrix(i)
      krigingPrediction(i) = predictor(pointPredict.x, pointPredict.y)
    }

    krigingPrediction
  }
}
*/
