package geotrellis.feature
import geotrellis.feature.op.geometry

import scala.collection.mutable
import org.apache.commons.math3.stat.regression.SimpleRegression

abstract sealed class ModelType
// Enumeration in Scala
case object Linear extends ModelType
case object Gaussian extends ModelType

object Variogram {
  case class Bucket(start:Double,end:Double) {
    private val points = mutable.Set[(Point[Int],Point[Int])]()

    def add(x:Point[Int],y:Point[Int]) = points += ((x,y))

    def contains(x:Double) =
      if(start==end) x == start
      else (start <= x) && (x < end)

    def midpoint = (start + end) / 2.0

    def semiVariance = {
      val sumOfSquares = 
        points.foldLeft(0.0){ case(acc,(x,y)) =>
          acc + math.pow((x.data-y.data),2)
        }
      sumOfSquares / points.size
    }

  }

  def makePairs[T](elements:List[T]):List[(T,T)] = {
    def f(elements:List[T],acc:List[(T,T)]):List[(T,T)] =
      elements match {
        case head :: List() =>
          acc
        case head :: tail =>
          f(tail,tail.map((head,_)) ++ acc)
        case _ => acc
      }
    f(elements,List[(T,T)]())
  }

  def apply(pts:Seq[Point[Int]],model:ModelType):Function1[Double,Double] = {
    val dmax = 100
    val lag = 10

    val distancePairs:Seq[(Double,(Point[Int],Point[Int]))] =
      makePairs(pts.toList)
        .map{ case(a,b) => (math.abs(a - b), (a,b)) }
        .filter { case (distance,_) => distance < dmax }
        .toSeq

    val buckets:Seq[Bucket] =
      if(lag == 0) {
        distancePairs
          .map{ case(d, _) => d }
          .distinct
          .map { d => Bucket(d,d) }
      } else {
        val n = dmax / lag
          (for((start,end) <- Seq.range(0,n-1,lag).zip(Seq.range(1,n,lag))) yield {
            Bucket(start,end)
          }).toSeq
      }

    for( (d,(x,y)) <- distancePairs ) {
      buckets.find(b => b.contains(d)) match {
        case Some(b) => b.add(x,y)
        case None => sys.error(s"Points $x and $y don't fit any bucket")
      }
    }

    val regressionPoints:Seq[(Double,Double)] = 
      buckets.map { b => (b.midpoint,b.semiVariance) }

    model match {
      case Linear =>
        // Construct slope and intercept
        val regression = new SimpleRegression
        for((x,y) <- regressionPoints) { regression.addData(x,y) }
        val slope = regression.getSlope
        val intercept = regression.getIntercept

        x => slope*x + intercept
      case Gaussian =>
        ???
    }
  }
}
