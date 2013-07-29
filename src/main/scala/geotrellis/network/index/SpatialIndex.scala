package geotrellis.network.index

import geotrellis.network._

import com.vividsolutions.jts.index.strtree.{STRtree, ItemDistance, ItemBoundable}
import com.vividsolutions.jts.index.strtree.ItemDistance
import com.vividsolutions.jts.geom.Coordinate
import com.vividsolutions.jts.geom.Envelope

import scala.collection.mutable
import scala.collection.JavaConversions._

import geotrellis.Extent

object SpatialIndex {
  def apply[T](points:Iterable[T])(f:T=>(Double,Double)):SpatialIndex[T] = {
    val si = new SpatialIndex[T](Measure.Dumb)(f)
    for(point <- points) {
      si.insert(point)
    }
    si
  }
}

class SpatialIndex[T](val measure:Measure)(f:T=>(Double,Double)) extends Serializable {
  val rtree = new STRtree
  val points = mutable.Set[T]()

  def insert(v:T) = {
    val (x,y) = f(v)
    rtree.insert(new Envelope(new Coordinate(x,y)), v)
    points.add(v)
  }

  def nearest(x:Double,y:Double):T = {
    rtree.nearestNeighbour(new Envelope(new Coordinate(x,y)),null,measure).asInstanceOf[T]
  }

  def nearest(location:Location):T = {
    val e = new Envelope(new Coordinate(location.long,location.lat))
    rtree.nearestNeighbour(e,null,measure).asInstanceOf[T]
  }

  def nearestInExtent(extent:Extent,location:Location):Option[T] = {
    val l = pointsInExtent(extent)
    if(l.isEmpty) { None }
    else {
      var nearest = l.head
      var minDist = {
        val (lat,long) = f(nearest)
        Distance.distance(location.lat,location.long,lat,long)
      }
      for(t <- l.tail) {
        val (lat,long) = f(t)
        val d = Distance.distance(location.lat,location.long,lat,long)
        if(d < minDist) {
          nearest = t
          minDist = d
        }
      }
      Some(nearest)
    }
  }

  def pointsInExtent(extent:Extent):Seq[T] = {
    rtree.query(new Envelope(extent.ymin,extent.ymax,extent.xmin,extent.xmax))
         .map(_.asInstanceOf[T])
  }

  def pointsInExtentAsJavaList(extent:Extent):List[_] = {
    rtree.query(new Envelope(extent.ymin,extent.ymax,extent.xmin,extent.xmax)).toList
  }
  
  
  def mergeIn(other:SpatialIndex[T]) = {
    for(point <- other.points) { insert(point) }
    this
  }
}

object Measure {
  def Dumb = new DumbMeasure
}

trait Measure extends ItemDistance with Serializable {
  def distance(x1:Double,y1:Double,x2:Double,y2:Double):Double

  def distance(i1:ItemBoundable, i2:ItemBoundable):Double = {
    val bound1 = i1.getBounds.asInstanceOf[Envelope]
    val bound2 = i2.getBounds.asInstanceOf[Envelope]
    distance(bound1.getMinX,bound1.getMinY,bound2.getMinX,bound2.getMinY)
  }
}

class DumbMeasure() extends Measure {
  def distance(x1:Double,y1:Double,x2:Double,y2:Double):Double = {
    val x = x2 - x1
    val y = y2 - y1
    math.sqrt(x*x + y*y)
  }
}
