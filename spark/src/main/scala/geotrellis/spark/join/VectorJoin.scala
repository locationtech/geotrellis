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
import geotrellis.util.annotations.experimental
import geotrellis.vector._

import com.vividsolutions.jts.geom.{ Envelope => JtsEnvelope }
import com.vividsolutions.jts.index.strtree.STRtree
import org.apache.spark._
import org.apache.spark.rdd._
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._

import scala.collection.JavaConverters._
import scala.reflect._

@experimental
object VectorJoin {
  @experimental
  def unflattened[
    L: ClassTag : ? => Geometry,
    R: ClassTag : ? => Geometry
  ](
    shorter: RDD[L],
    longer: RDD[R],
    pred: (Geometry, Geometry) => Boolean
  )(implicit sc: SparkContext): RDD[(L, Seq[R])] = {
    val rtrees = longer.mapPartitions({ partition =>
      val rtree = new STRtree

      partition.foreach({ r =>
        val Extent(xmin, ymin, xmax, ymax) = r.envelope
        val envelope = new JtsEnvelope(xmin, xmax, ymin, ymax)
        rtree.insert(envelope, r)
      })

      Iterator(rtree)
    }, preservesPartitioning = true)

    shorter.cartesian(rtrees).map({ case (left, tree) =>
      val Extent(xmin, ymin, xmax, ymax) = left.envelope
      val envelope = new JtsEnvelope(xmin, xmax, ymin, ymax)
      val rights = tree.query(envelope)
        .asScala
        .map({ right: Any => right.asInstanceOf[R] })
        .filter({ right => pred(left, right) })

      (left, rights)
    })
  }

  def apply[
    L: ClassTag : ? => Geometry,
    R: ClassTag : ? => Geometry
  ](
    shorter: RDD[L],
    longer: RDD[R],
    pred: (Geometry, Geometry) => Boolean
  )(implicit sc: SparkContext): RDD[(L, R)] =
    unflattened(shorter, longer, pred)
      .flatMap({ case (left, rights) => rights.map({ right => (left, right) }) })

}
