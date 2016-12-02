/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.spark.join

import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.vector._

import org.apache.spark._
import org.apache.spark.rdd._
import com.vividsolutions.jts.geom.Envelope

import scala.reflect._


object VectorJoin {

  /**
    * A function which calculates the envelope of a partition.
    *
    * @param  i   The index of the partition
    * @param  gs  An iterator containing the contents of the RDD
    * @return     An Iterator containing one index-envelope pair
    */
  def calculateEnvelope[T : ? => Geometry](
    i: Int,
    gs: Iterator[T]
  ): Iterator[(Int, Envelope)] = {
    val env = gs.foldLeft(new Envelope)({ (env: Envelope, g: T) =>
      val Extent(xmin, ymin, xmax, ymax) = g.envelope
      val env2 = new Envelope(xmin, xmax, ymin, ymax)
      env.expandToInclude(env2)
      env
    })

    Iterator((i, env))
  }

  /**
    * Perform the vector join operation over an RDD[L] and and RDD[R],
    * where both L and R are viewble as Geometry.  This makes use of
    * the FilteredCartesianRDD type to accelerate the process
    * (relative to plain-old CartesianRDD).
    *
    * @param  left   An RDD[L], where L is viewable as a Geometry
    * @param  right  An RDD[R], where R is viewable as a Geometry
    * @param  pred   A predicate which answers whether an L and an R should be joined
    * @return        An RDD of L-R pairs
    */
  def apply[
    L: ClassTag : ? => Geometry,
    R: ClassTag : ? => Geometry
  ](
    left: RDD[L],
    right: RDD[R],
    pred: (Geometry, Geometry) => Boolean
  )(implicit sc: SparkContext): RDD[(L, R)] = {

    val leftEnvs = left.mapPartitionsWithIndex(calculateEnvelope[L], true).collect.toMap
    val rightEnvs = right.mapPartitionsWithIndex(calculateEnvelope[R], true).collect.toMap
    val prePred: (Int, Int) => Boolean = { (i, j) =>
      val env1 = leftEnvs.getOrElse(i, new Envelope)
      val env2 = rightEnvs.getOrElse(j, new Envelope)
      env1.intersects(env2)
    }

    (new FilteredCartesianRDD(sc, prePred, left, right))
      .filter({ case (left: L, right: R) => pred(left, right) })
  }

  /**
    * Perform the vector join operation over an RDD[L] and and RDD[R],
    * where both L and R are viewble as Geometry.
    *
    * @param  left   An RDD[L], where L is viewable as a Geometry
    * @param  right  An RDD[R], where R is viewable as a Geometry
    * @param  pred   A predicate which answers whether an L and an R should be joined
    * @return        An RDD of L-R pairs
    */
  def naive[
    L: ClassTag : ? => Geometry,
    R: ClassTag : ? => Geometry
  ](
    left: RDD[L],
    right: RDD[R],
    pred: (Geometry, Geometry) => Boolean
  )(implicit sc: SparkContext): RDD[(L, R)] =
    left.cartesian(right)
      .filter({ case (left: L, right: R) => pred(left, right) })

}
