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

import geotrellis.vector._
import geotrellis.spark._
import geotrellis.spark.tiling._

import com.vividsolutions.jts.geom.{ Envelope => JtsEnvelope }
import com.vividsolutions.jts.index.strtree.STRtree
import org.apache.spark._
import org.apache.spark.rdd._
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._

import scala.collection.JavaConverters._
import scala.reflect._


object VectorJoin {

  def apply[
    L: ClassTag : ? => Geometry,
    R: ClassTag : ? => Geometry
  ](
    longer: RDD[L],
    shorter: RDD[R],
    pred: (Geometry, Geometry) => Boolean
  )(implicit sc: SparkContext): RDD[(L, R)] = {
    val rtrees = shorter.mapPartitions({ partition =>
      val rtree = new STRtree

      partition.foreach({ r =>
        val Extent(xmin, ymin, xmax, ymax) = r.envelope
        val envelope = new JtsEnvelope(xmin, xmax, ymin, ymax)
        rtree.insert(envelope, r)
      })

      Iterator(rtree)
    }, preservesPartitioning = true)
      .zipWithIndex
      .map({ case (v, k) => (k, v) })
      .cache

    val count = rtrees.count.toInt

    // For every partition of the right-hand (smaller) collection of
    // items, find an RDD of items from the longer collection that
    // intersects with some member of that partition partition.
    val rdds = (0 until count).map({ i =>
      val tree = sc.broadcast(rtrees.lookup(i).head)

      longer.flatMap({ l =>
        val Extent(xmin, ymin, xmax, ymax) = l.envelope
        val envelope = new JtsEnvelope(xmin, xmax, ymin, ymax)

        tree.value.query(envelope)
          .asScala
          .map({ r: Any => r.asInstanceOf[R] })
          .filter({ r => pred(l, r) })
          .map({ r => (l, r) })
      })
    })

    // Return the results as a single RDD
    sc.union(rdds)
  }
}
