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

package geotrellis.vector

import com.vividsolutions.jts.index.strtree.{STRtree, ItemDistance, ItemBoundable}
import com.vividsolutions.jts.index.strtree.ItemDistance
import com.vividsolutions.jts.geom.Coordinate
import com.vividsolutions.jts.geom.Envelope

import scala.collection.mutable
import scala.collection.JavaConversions._

object SpatialIndex {
  def apply(points: Iterable[(Double, Double)])
           (implicit di: DummyImplicit): SpatialIndex[(Double, Double)] = {
    val si = new SpatialIndex[(Double, Double)](Measure.Dumb)
    for(point <- points) {
      si.insert(point, point._1, point._2)
    }
    si
  }

  def apply[T](points: Iterable[T])(f: T=>(Double, Double)): SpatialIndex[T] = {
    val si = new SpatialIndex[T](Measure.Dumb)
    for(point <- points) {
      val (x, y) = f(point)
      si.insert(point, x, y)
    }
    si
  }
}

class SpatialIndex[T](val measure: Measure) extends Serializable {
  val rtree = new STRtree
  val points = mutable.Set[T]()

  def insert(v: T, x: Double, y: Double) = {
    rtree.insert(new Envelope(new Coordinate(x, y)), v)
    points.add(v)
  }

  def nearest(x: Double, y: Double): T = {
    rtree.nearestNeighbour(new Envelope(new Coordinate(x, y)), null, measure).asInstanceOf[T]
  }

  def nearest(pt: (Double, Double)): T = {
    val e = new Envelope(new Coordinate(pt._1, pt._2))
    rtree.nearestNeighbour(e, null, measure).asInstanceOf[T]
  }

  def pointsInExtent(extent: Extent): Seq[T] = {
    rtree.query(new Envelope(extent.xmin, extent.xmax, extent.ymin, extent.ymax))
         .map(_.asInstanceOf[T])
  }

  def pointsInExtentAsJavaList(extent: Extent): List[_] = {
    rtree.query(new Envelope(extent.xmin, extent.xmax, extent.ymin, extent.ymax)).toList
  }
}

object Measure {
  def Dumb = new DumbMeasure
}

trait Measure extends ItemDistance with Serializable {
  def distance(x1: Double, y1: Double, x2: Double, y2: Double): Double

  def distance(i1: ItemBoundable, i2: ItemBoundable): Double = {
    val bound1 = i1.getBounds.asInstanceOf[Envelope]
    val bound2 = i2.getBounds.asInstanceOf[Envelope]
    distance(bound1.getMinX, bound1.getMinY, bound2.getMinX, bound2.getMinY)
  }
}

class DumbMeasure() extends Measure {
  def distance(x1: Double, y1: Double, x2: Double, y2: Double): Double = {
    val x = x2 - x1
    val y = y2 - y1
    math.sqrt(x*x + y*y)
  }
}
