package geotrellis.feature
import geotrellis.feature.op.geometry


// abstract sealed class ModelType
// // Enumeration in Scala
// case object Linear extends ModelType


// trait Variogram(ps:Seq[Point[Int]], model:ModelType) {
object Variogram(ps:Seq[Point[Int]]) {
  // possibly placeholders for parameters
  val dmax = 100
  val lag = 10

  // val pairs = ps.flatMap(a => ps.map(b => (a,b)))
  val pairs = makePairs(ps)

  // .filter{ case (i,j) => GetDistance(i,j) <= dmax }
  val distancePairs = pairs.map{ case(a,b) => (GetDistance(a,b), (a,b)) }
  val distances = distancePairs.map{ case(d, _) => d._1 }.distinct
  val semiVariances = distances
    .map( a => (a,computeSemiVariance(
      distancePairs.filter{case(d,_) => d==a}.map{case (_,p) => p} )))

  val n = dmax / lag
  val buckets = Seq.range(0,n-1,lag).zip(Seq.range(1,n,lag))
  val semiVariances2 =  buckets.map( a => (a,computeSemiVariance(
      distancePairs.filter{case(d,_) => (d >= a._1 && d < a._2)}.map{case (_,p) => p} )))


  def computeSemiVariance(pair:Seq[(Point[Int],Point[Int])]):Double = {
    pair.foldLeft(0){ case(b,a) => (a._1.data-a._2.data)^2 + b }
  }
  def makePairs(ps:Seq[Point[Int]]):Seq[([Point[Int]],[Point[Int]])] = {
    ps.foldLeft((Seq.empty[([Point[Int]],[Point[Int]])],ps.tail)) {
      case(b,a) => (a.map{ x => (a,x) } ++ b._1,
                    b._2.tail) }._1
  }
  def getSemiVariance(d:Double):Double = {
    semiVariances.filter{ case(a,_) => a == d }._1
  }
  def getSemiVariance2(d:Double):Double = {
    semiVariances2.filter{ case(a,_) => (d >= a._1 && d < a._2) }._1
  }
}

// Seq[Point[Int]] => (Double => Double)

// ps.zip(ps)

// (a,b) (a,c) (a,d) (b,c) ...

// dmin dmax

// take all tuples of points where dmin <= d(a,b) <= max

// val variance = {a,b,c,d}.variance

// 0 to DMAX
// chop it up into buckets
// compute the variance for each bucket

// val e empirical semivariogram:
// do linear regression (ordinary least squares) on e.
