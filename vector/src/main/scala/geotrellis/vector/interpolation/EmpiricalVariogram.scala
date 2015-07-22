package geotrellis.vector.interpolation

import geotrellis.vector._
import org.apache.commons.math3.linear.{MatrixUtils, RealMatrix}

import spire.syntax.cfor._

import scala.collection.mutable

class EmpiricalVariogram(length: Int) {
  val distances = Array.ofDim[Double](length)
  val variance = Array.ofDim[Double](length)
}

object EmpiricalVariogram {

  def nonlinear(pts: Seq[PointFeature[Double]], maxdist: Double, binmax: Int): EmpiricalVariogram  =
    NonLinearEmpiricalVariogram(pts, maxdist, binmax)
  
  def linear(pts: Seq[PointFeature[Double]], radius: Option[Double] = None, lag: Double = 0.0): Array[(Double, Double)] =
    LinearEmpiricalVariogram(pts, radius, lag)
}

object NonLinearEmpiricalVariogram {
  def apply(pts: Seq[PointFeature[Double]], maxdist: Double, binmax: Int) = {
    val points = pts.toArray
    val n: Int = points.length

    val distances =
      new mutable.PriorityQueue[(Int, Int, Double)]()(Ordering.by(-1 * _._3))

    var dMax = Double.MinValue

    cfor(0)(_ < n, _ + 1) { i: Int =>
      cfor(i + 1)(_ < n, _ + 1) { j: Int =>
        val dx = pts(i).geom.x - pts(j).geom.x
        val dy = pts(i).geom.y - pts(j).geom.y
        val d = math.sqrt(dx * dx + dy * dy)
        if(maxdist == 0) {
          if(d > dMax) dMax = d
          distances += ((i, j, d))
        } else {
          if(d <= maxdist) {
            distances += ((i, j, d))
          }
        }
      }
    }

    val sortedDistances: Array[(Int, Int, Double)] = {
      val q = distances.dequeueAll
      if(maxdist == 0) {
        val md = dMax / 2.0
        val result = q.takeWhile(_._3 <= md).toArray
        if(result.length == 0) {
          // This is a strangely uniform dataset.
          // Assume that the maxDistances is the
          // actual maximum distance in the dataset.
          q.toArray
        } else {
          result
        }
      } else {
        val result = q.toArray
        if(result.length == 0) {
          throw new IllegalArgumentException("No points in the dataset with a distance below $maxDistance")
        }
        result
      }
    }
    val n0_S: Int = sortedDistances.length
    val binMax: Int = if(binmax == 0) 100 else binmax
    val binSize: Int = math.ceil(n0_S * 1.0 / binMax).toInt
    val binNum: Int = if(binSize >= 30) binMax else math.ceil(n0_S/30).toInt
    val empiricalSemivariogram = new EmpiricalVariogram(binNum)
    val Z: Array[Double] = Array.tabulate(n){j => pts(j).data}

    cfor(0)(_ < binNum, _ + 1) { i: Int =>
      val n0: Int = i * binSize + 1 - 1
      val n1Temp: Int = (i + 1) * binSize - 1
      val n1: Int = if (n1Temp > n0_S) n0_S - 1 else n1Temp
      val binSizeLocal: Int = n1 - n0 + 1
      val S1: Array[Int] = Array.tabulate(n1 - n0 + 1) { j => sortedDistances(n0 + j)._1 }
      val S2: Array[Int] = Array.tabulate(n1 - n0 + 1) { j => sortedDistances(n0 + j)._2 }
      val Li: Double = Array.tabulate(n1 - n0 + 1) { j => sortedDistances(n0 + j)._3 }.sum / binSizeLocal
      val Vi: Double = Array.tabulate(n1 - n0 + 1) { j =>
        math.pow(Z(S1(j)) - Z(S2(j)), 2)
      }.sum / (2 * binSizeLocal)
      empiricalSemivariogram.distances(i) = Li
      empiricalSemivariogram.variance(i) = Vi
    }
    empiricalSemivariogram
  }
}

object LinearEmpiricalVariogram {
  case class Bucket(start: Double, end: Double) {
    private val points = mutable.Set[(PointFeature[Double], PointFeature[Double])]()

    def add(x: PointFeature[Double], y: PointFeature[Double]) = points += ((x, y))

    def contains(x: Double) =
      if(start==end) x == start
      else (start <= x) && (x < end)

    def midpoint = (start + end) / 2.0
    def isEmpty = points.isEmpty
    def semivariance = {
      val sumOfSquares =
        points.foldLeft(0.0){ case(acc, (x, y)) =>
          acc + math.pow(x.data - y.data, 2)
        }
      (sumOfSquares / points.size) / 2
    }
  }


  /** Produces unique pairs of points */
  def makePairs[T](elements: List[T]): List[(T, T)] = {
    def f(elements: List[T], acc: List[(T, T)]): List[(T, T)] =
      elements match {
        case head :: List() =>
          acc
        case head :: tail =>
          f(tail, tail.map((head, _)) ++ acc)
        case _ => acc
      }
    f(elements, List[(T, T)]())
  }

  def apply(pts: Seq[PointFeature[Double]], radius: Option[Double] = None, lag: Double = 0.0): Array[(Double, Double)] = {
    def distance(p1: Point, p2: Point) = math.abs(math.sqrt(math.pow(p1.x - p2.x, 2) + math.pow(p1.y - p2.y, 2)))

    // every pair of points and their distance from each other
    val distancePairs: Seq[(Double, (PointFeature[Double], PointFeature[Double]))] = {
      val pairs = makePairs(pts.toList).map { case (a, b) => (distance(a.geom, b.geom), (a, b)) }
      radius match {
        case Some(dmax) =>
          pairs
            .filter { case (distance, _) => distance <= dmax }
            .toSeq
        case None =>
          pairs
            .toSeq
      }
    }

    val buckets: Seq[Bucket] =
      if(lag == 0) {
        distancePairs
          .map{ case(d, _) => d }
          .distinct
          .map { d => Bucket(d, d) }
      } else {
        // the maximum distance between two points in the field
        val dmax: Double = distancePairs.map{ case(d, _) => d }.max
        // the lower limit of the largest bucket
        val E = 1e-4
        val lowerLimit: Double =
          (Math.floor(dmax / lag).toInt * lag) + 1

        (0.0 to lowerLimit by lag).toList
          .zip((lag to (lowerLimit + lag) by lag) toList)
          .map{ case(start, end) => Bucket(start, end) }
      }

    // populate the buckets
    for( (d, (x, y)) <- distancePairs ) {
      buckets.find(b => b.contains(d)) match {
        case Some(b) => b.add(x, y)
        case None => sys.error(s"Points $x and $y don't fit any bucket")
      }
    }

    buckets
      .filter { b => !b.isEmpty } // empty buckets are first removed
      .map { b => (b.midpoint, b.semivariance) } // use midpoint of buckets for distance
      .toArray
  }
}