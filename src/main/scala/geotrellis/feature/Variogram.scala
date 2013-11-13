package geotrellis.feature
import geotrellis.feature.op.geometry


abstract sealed class ModelType
// Enumeration in Scala
case object Linear extends ModelType
case object Gaussian extends ModelType

class Variogram(ps:Seq[Point[Int]], model:ModelType) {
// object Variogram(ps:Seq[Point[Int]]) {
  // possibly placeholders for parameters
  val dmax = 100
  val lag = 10

  // val pairs = ps.flatMap(a => ps.map(b => (a,b)))
  val pairs = makePairs(ps)

  // .filter{ case (i,j) => GetDistance(i,j) <= dmax }
  val distancePairs = 
    pairs.map{ case(a,b) => (GetDistance(a,b), (a,b)) }
  val distances = distancePairs.map{ case(d, _) => d._1 }.distinct
  
  val semivariances = 
    if (lag == 0) {
      distances.map( a => ((a,a),computeSemivariance(
        distancePairs.filter{ case(d,_) => d==a }.map{case (_,p) => p} )))
    } else {
      val n = dmax / lag
      val buckets = Seq.range(0,n-1,lag).zip(Seq.range(1,n,lag))
      // make fix: iterate distance pairs -> buckets
      buckets.map( a => (a,computeSemivariance(
        distancePairs.filter{case(d,_) => (d >= a._1 && d < a._2)}.map{case (_,p) => p} )))
    }

  def computeSemivariance(pair:Seq[(Point[Int],Point[Int])]):Double = {
    pair.foldLeft(0){ case(b,a) => (a._1.data-a._2.data)^2 + b }
  }
  def makePairs(ps:Seq[Point[Int]]):Seq[([Point[Int]],[Point[Int]])] = {
    ps.foldLeft((Seq.empty[([Point[Int]],[Point[Int]])],ps.tail)) {
      case(b,a) => (a.map{ x => (a,x) } ++ b._1,
                    b._2.tail) }._1
  }
  // def getSemivariance(d:Double):Double = {
  //   semivariances.filter{ case(a,_) => a == d }._1
  // }
  // def getSemivariance2(d:Double):Double = {
  //   semivariances2.filter{ case(a,_) => (d >= a._1 && d < a._2) }._1
  // }

  // y_fit = b_0 + b_1*Xi
  // b_1 = [n*Σ(Xi*Yi) - Σ(Xi)*Σ(Yi)]  /  [n*Σ(Xi^2) - Σ(Xi)^2]
  // b_0 = [Σ(Xi^2)*Σ(Yi) - Σ(Xi)*Σ(Xi*Yi)] / [n*Σ(Xi^2) - Σ(Xi)^2]
  // val regressionPoints = semivariances
  val regressionPoints = semivariances.map{case((l,r),sv) => ((r+l)/2,sv)}

  val n_0 = regressionPoints.size
  val t_0 = regressionPoints.foldLeft(0){ case(b,(x,y)) => (x*y) + b }
  val t_1 = regressionPoints.foldLeft(0){ case(b,(x,y)) => x + b }
  val t_2 = regressionPoints.foldLeft(0){ case(b,(x,y)) => y + b }
  val t_3 = regressionPoints.foldLeft(0){ case(b,(x,y)) => x^2 + b }
  val slope = (n_0*t_0 - t_1*t_2) / (n_0*t_3 - t_1^2)
  val intercept = ((t_3^2)*t_2 - t_1*t_0) / (n_0*t_3 - t_1^2)

  // get semivariance from simple linear regression model
  def getSemivariance(x:Double):Double = {
    slope*x + intercept
  }

  // Return semiovariogram
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
