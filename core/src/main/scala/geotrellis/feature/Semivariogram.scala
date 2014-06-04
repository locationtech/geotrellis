/*
 * Copyright (c) 2014 Azavea.
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

package geotrellis.feature

import geotrellis._
import geotrellis.raster.NODATA

import scala.collection.mutable
import org.apache.commons.math3.stat.regression.SimpleRegression

abstract sealed class ModelType

case object Linear extends ModelType
case object Gaussian extends ModelType
case object Circular extends ModelType
case object Spherical extends ModelType
case object Exponential extends ModelType

/**
  Empirical semivariogram
*/
object Semivariogram {
  case class Bucket(start:Double,end:Double) {
    private val points = mutable.Set[(PointFeature[Int],PointFeature[Int])]()

    def add(x:PointFeature[Int],y:PointFeature[Int]) = points += ((x,y))

    def contains(x:Double) =
      if(start==end) x == start
      else (start <= x) && (x < end)

    def midpoint = (start + end) / 2.0
    def isEmpty = points.isEmpty
    def semivariance = {
      val sumOfSquares = 
        points.foldLeft(0.0){ case(acc,(x,y)) =>
          acc + math.pow((x.data-y.data),2)
        }
      (sumOfSquares / points.size) / 2
    }
  }

  /** Produces unique pairs of points */
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

  def apply(pts:Seq[PointFeature[Int]],radius:Option[Int]=None,lag:Int=0,model:ModelType):Function1[Double,Double] = {
    def distance(p1: Point, p2: Point) = math.abs(math.sqrt(math.pow(p1.x - p2.x,2) + math.pow(p1.y - p2.y,2)))

    // ignore points without a value
    val validPts = pts.filter(pt => pt.data != NODATA)

    // every pair of points and their distance from each other
    val distancePairs:Seq[(Double,(PointFeature[Int],PointFeature[Int]))] =
      radius match {
        case Some(dmax) =>
          makePairs(validPts.toList)
            .map{ case(a,b) => (distance(a.geom, b.geom), (a,b)) }
            .filter { case (distance,_) => distance <= dmax }
            .toSeq
        case None =>
            makePairs(validPts.toList)
              .map{ case(a,b) => (distance(a.geom, b.geom), (a,b)) }
              .toSeq
      }

    val buckets:Seq[Bucket] =
      if(lag == 0) {
        distancePairs
          .map{ case(d,_) => d }
          .distinct
          .map { d => Bucket(d,d) }
      } else {
        // the maximum distance between two points in the field
        val dmax = distancePairs.map{ case(d,_) => d }.max
        // the lower limit of the largest bucket
        val lowerLimit = (Math.floor(dmax/lag).toInt * lag) + 1
        List.range(0,lowerLimit,lag).zip(List.range(lag,lowerLimit+lag,lag))
          .map{ case(start,end) => Bucket(start,end) }
      }

    // populate the buckets
    for( (d,(x,y)) <- distancePairs ) {
      buckets.find(b => b.contains(d)) match {
        case Some(b) => b.add(x,y)
        case None => sys.error(s"Points $x and $y don't fit any bucket")
      }
    }

    // use midpoint of buckets for distance
    val regressionPoints:Seq[(Double,Double)] = 
      // empty buckets are first removed
      buckets.filter ( b => !b.isEmpty)
        .map { b => (b.midpoint,b.semivariance) }

    model match {
      case Linear =>
        // Construct slope and intercept
        val regression = new SimpleRegression
        for((x,y) <- regressionPoints) { regression.addData(x,y) }
        val slope = regression.getSlope
        val intercept = regression.getIntercept
        x => slope*x + intercept

      case Gaussian => ???
      case Exponential => ???
      case Circular => ???
      case Spherical => ???
    }
  }
}
