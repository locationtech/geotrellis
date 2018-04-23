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

package geotrellis.vector

import org.locationtech.jts.index.strtree.{STRtree, ItemDistance, ItemBoundable, AbstractNode}
import org.locationtech.jts.index.strtree.ItemDistance
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.operation.distance.DistanceOp

import scala.collection.mutable
import scala.collection.JavaConversions._

object SpatialIndex {
  def apply(points: Traversable[(Double, Double)]): SpatialIndex[(Double, Double)] = {
    val si = new SpatialIndex[(Double, Double)](Measure.Euclidean)
    for(point <- points) {
      si.insert(point, point._1, point._2)
    }
    si
  }

  def apply[T](points: Traversable[T])(f: T=>(Double, Double)): SpatialIndex[T] = {
    val si = new SpatialIndex[T](Measure.Euclidean)
    for(point <- points) {
      val (x, y) = f(point)
      si.insert(point, x, y)
    }
    si
  }

  def fromExtents[T](items: Traversable[T])(f: T => Extent): SpatialIndex[T] = {
    val idx = new SpatialIndex[T]
    items.foreach { i => idx.insert(i, f(i)) }
    idx
  }
}

class SpatialIndex[T](val measure: Measure = Measure.Euclidean) extends Serializable {
  val rtree = new STRtree
  val points = mutable.Set.empty[T]

  def insert(v: T, x: Double, y: Double) = {
    rtree.insert(new Envelope(new Coordinate(x, y)), v)
    points.add(v)
  }

  def insert(v: T, ex: Extent) = {
    rtree.insert(ex.jtsEnvelope, v)
    points.add(v)
  }

  def nearest(x: Double, y: Double): T =
    rtree.nearestNeighbour(new Envelope(new Coordinate(x, y)), null, measure).asInstanceOf[T]

  def nearest(pt: (Double, Double)): T = {
    val e = new Envelope(new Coordinate(pt._1, pt._2))
    rtree.nearestNeighbour(e, null, measure).asInstanceOf[T]
  }

  def nearest(ex: Extent): T =
    rtree.nearestNeighbour(ex.jtsEnvelope, null, measure).asInstanceOf[T]

  def traversePointsInExtent(extent: Extent): Traversable[T] =
    new Traversable[T] {
      override def foreach[U](f: T => U): Unit = {
        val visitor = new org.locationtech.jts.index.ItemVisitor {
          override def visitItem(obj: AnyRef): Unit = f(obj.asInstanceOf[T])
        }
        rtree.query(extent.jtsEnvelope, visitor)
      }
    }

  def pointsInExtent(extent: Extent): Vector[T] =
    traversePointsInExtent(extent).to[Vector]

  def pointsInExtentAsJavaList(extent: Extent): java.util.List[T] =
    rtree.query(new Envelope(extent.xmin, extent.xmax, extent.ymin, extent.ymax)).asInstanceOf[java.util.List[T]]

  def kNearest(x: Double, y: Double, k: Int): Seq[T] =
    kNearest(new Envelope(new Coordinate(x, y)), k)

  def kNearest(pt: (Double, Double), k: Int): Seq[T] =
    kNearest(new Envelope(new Coordinate(pt._1, pt._2)), k)

  def kNearest(ex: Extent, k: Int): Seq[T] = {
    case class PQitem[A](val d: Double, val x: A) extends Ordered[PQitem[A]] {
      def compare(that: PQitem[A]) = (this.d) compare (that.d)
    }

    val gf = new GeometryFactory()
    val env = ex.jtsEnvelope
    val pq = (new mutable.PriorityQueue[PQitem[AbstractNode]]()).reverse
    val kNNqueue = new mutable.PriorityQueue[PQitem[T]]()

    def addToClosest (elem: PQitem[T]) = {
      if (kNNqueue.size < k)
        kNNqueue.enqueue(elem)
      else {
        if (elem.d < kNNqueue.head.d) {
          kNNqueue.enqueue(elem)
          kNNqueue.dequeue()
        }
      }
    }
    def rtreeLeafAsPQitem (ib: ItemBoundable): PQitem[T] = {
      PQitem(DistanceOp.distance(gf.toGeometry(env), gf.toGeometry(ib.getBounds.asInstanceOf[Envelope])), ib.getItem.asInstanceOf[T])
    }
    def rtreeNodeAsPQitem (nd: AbstractNode): PQitem[AbstractNode] = {
      PQitem(DistanceOp.distance(gf.toGeometry(env), gf.toGeometry(nd.getBounds.asInstanceOf[Envelope])), nd)
    }

    pq.enqueue(rtreeNodeAsPQitem(rtree.getRoot))

    do {
      val item = pq.dequeue

      if (kNNqueue.size < k || item.d < kNNqueue.head.d) {
        if (item.x.getLevel == 0) {
          // leaf node
          item.x.getChildBoundables.map {
            leafNode => rtreeLeafAsPQitem(leafNode.asInstanceOf[ItemBoundable])
          }.foreach(addToClosest)
        } else {
          item.x.getChildBoundables.map {
            subtree => rtreeNodeAsPQitem(subtree.asInstanceOf[AbstractNode])
          }.foreach(pq.enqueue(_))
        }
      }
    } while (! pq.isEmpty )

    kNNqueue.toSeq.map{ _.x }
  }
}

object Measure {
  def Euclidean = new EuclideanMeasure
}

trait Measure extends ItemDistance with Serializable {
  def distance(x1: Double, y1: Double, x2: Double, y2: Double): Double

  def distance(i1: ItemBoundable, i2: ItemBoundable): Double = {
    val bound1 = i1.getBounds.asInstanceOf[Envelope]
    val bound2 = i2.getBounds.asInstanceOf[Envelope]
    distance(bound1.getMinX, bound1.getMinY, bound2.getMinX, bound2.getMinY)
  }
}

class EuclideanMeasure() extends Measure {
  def distance(x1: Double, y1: Double, x2: Double, y2: Double): Double = {
    val x = x2 - x1
    val y = y2 - y1
    math.sqrt(x*x + y*y)
  }
}
