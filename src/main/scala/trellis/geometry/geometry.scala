package trellis.geometry

import scala.collection.mutable.ArrayBuffer
import scala.math.{min, max, round}

import trellis.geometry.grid.{GridPoint, GridLine, GridPolygon}
import trellis.{Extent,RasterExtent}
import trellis.raster.{IntRaster}

import com.vividsolutions.jts.geom.{GeometryFactory,Geometry}
import com.vividsolutions.jts.geom.{Coordinate => JtsCoordinate}
import com.vividsolutions.jts.geom.{Point      => JtsPoint}
import com.vividsolutions.jts.geom.{LineString => JtsLineString}
import com.vividsolutions.jts.geom.{LinearRing => JtsLinearRing}
import com.vividsolutions.jts.geom.{Polygon    => JtsPolygon}
import com.vividsolutions.jts.geom.{Geometry   => JtsGeometry}

/**
 * Turn tuples into JtsCoordinates.
 */
trait UsesCoords {
  def makeCoord(x:Double, y:Double) = { new JtsCoordinate(x, y) }

  def makeCoords(tpls:Array[(Double, Double)]) = {
    tpls.map { pt => makeCoord(pt._1, pt._2) }.toArray
  }
}

/** 
 * Features are pieces of geometry with a value and/or attributes. If you don't
 * need the value and/or the attrs, you can use things like 0 and null.
 */
trait Feature extends UsesCoords {
  val attrs:Map[String, Any]
  val value:Int

  def describe() = "Feature"
}

/**
 * Factory wraps GeometryFactory, which is used to build various JTS objects.
 */
object Factory extends UsesCoords {
  val f = new GeometryFactory()
}

/**
 * Point represents a simple (x,y) coordinate.
 */
class Point(val jts:JtsPoint, val value:Int,
            val attrs:Map[String, Any]) extends Feature {
  val x:Double = this.jts.getX
  val y:Double = this.jts.getY
}

object Point extends UsesCoords {
  def apply(x:Double, y:Double, value:Int, attrs:Map[String, Any]) = {
    new Point(Factory.f.createPoint(makeCoord(x, y)), value, attrs)
  }

  def apply(coord:JtsCoordinate, value:Int, attrs:Map[String, Any]) = {
    new Point(Factory.f.createPoint(coord), value, attrs)
  }
}

/**
 * A line string is a a series of (x, y) points connected by line segments.
 */
class LineString(val jts:JtsLineString, val value:Int,
                 val attrs:Map[String, Any]) extends Feature

object LineString extends UsesCoords {
  def apply(tpls:Array[(Double, Double)], value:Int,
            attrs:Map[String, Any]): LineString = {
    val coords = makeCoords(tpls)
    LineString(coords, value, attrs)
  }
  
  def apply(pts:Array[Point], value:Int,
            attrs:Map[String, Any]): LineString = {
    val coords = pts.map { pt => pt.jts.getCoordinate }
    LineString(coords, value, attrs)
  }

  def apply(coords:Array[JtsCoordinate], value:Int,
            attrs:Map[String, Any]): LineString = {
    val jts    = Factory.f.createLineString(coords)
    new LineString(jts, value, attrs)
  }
}

/**
 * A polygon is a series of (x,y) points connected by line segments
 * (like LineString) but with the last and first points also connected. Unlike
 * Point and LineString, Polygon encloses an area.
 */
class Polygon(val jts:JtsPolygon, val value:Int,
              val attrs:Map[String, Any]) extends Feature {
  val (xmin, ymin) = this.getCoordTuples.reduceLeft {
    (a, b) => (min(a._1, b._1), min(a._2, b._2))
  }
  val (xmax, ymax) = this.getCoordTuples.reduceLeft {
    (a, b) => (max(a._1, b._1), max(a._2, b._2))
  }

  def getCoordinates() = {
    this.jts.getExteriorRing.getCoordinates
  }
  def getCoordTuples() = {
    this.jts.getExteriorRing.getCoordinates.map { c => (c.x, c.y) }
  }
  def translateToGrid(geo:RasterExtent):Option[GridPolygon] = {
    val coords = this.getCoordinates
    val pts:Array[GridPoint] = coords.map {
      pt => {
        //val (col, row) = geo.mapToGrid(pt.x, pt.y)
        val (col, row) = geo.mapToGrid2(pt.x, pt.y)
        GridPoint(col, row)
      }
    }

    if (pts.length < 3) return None

    // figure out which points to use
    val pts2 = ArrayBuffer.empty[GridPoint]
    var lst = pts(0)
    pts2.append(lst)

    var i = 1
    while (i < pts.length) {
      val nxt = pts(i)
      i += 1
      if (lst.compare(nxt) != 0) {
        pts2.append(nxt)
        lst = nxt
      }
    }

    if (pts2.length > 3) {
      //printf("LENGTH IS %d\n", pts2.length)
      //printf("POINTS ARE %s\n", pts2.toList)
      Some(GridPolygon(pts2.toArray))
    } else {
      None
    }
  }

  def buildFromJtsGeometry(j:JtsGeometry) = j match {
    case j:JtsPolygon => Some(new Polygon(j, 0, Map.empty[String, Any]))
    case _ => None
  }

  def intersection(other:Polygon) = {
    throw new Exception("WTF WTF WTF")
    val g = this.jts.intersection(other.jts)
    val n = g.getNumGeometries()
    if (g.isEmpty()) {
      List.empty[Polygon]
    } else if (n == 1) {
      List(buildFromJtsGeometry(g)).flatten
    } else {
      //println("MULTIPOLYGON: %d GEOMETRIES", n)
      (0 until n).toList.map {
        i => buildFromJtsGeometry(g.getGeometryN(i))
      }.flatten
    }
  }

  def bound(e:Extent) = {
    val pts = Array((e.xmin, e.ymin), (e.xmax, e.ymin),
                    (e.xmax, e.ymax), (e.xmin, e.ymax),
                    (e.xmin, e.ymin))
    val bound = Polygon(pts, 0, Map.empty[String, Any])
    this.intersection(bound)
  }

  def getExtent() = Extent(xmin, ymin, xmax, ymax)

  def getBounds():(Double, Double, Double, Double) = (xmin, ymin, xmax, ymax)
}

object Polygon extends UsesCoords {
  def apply(tpls:Array[(Double, Double)], value:Int,
            attrs:Map[String, Any]): Polygon = {
    val coords = makeCoords(tpls)
    Polygon(coords, value, attrs)
  }

  def apply(pts:Array[Point], value:Int,
            attrs:Map[String, Any]): Polygon = {
    val coords = pts.map { pt => pt.jts.getCoordinate }
    Polygon(coords, value, attrs)
  }

  def apply(coords:Array[JtsCoordinate], value:Int,
            attrs:Map[String, Any]): Polygon = {
    val shell  = Factory.f.createLinearRing(coords)
    val holes  = Array[JtsLinearRing]()
    val jts    = Factory.f.createPolygon(shell, holes)
    new Polygon(jts, value, attrs)
  }
}

case class MultiPolygon(polygons:Array[Polygon], value:Int, attrs: Map[String,Any]) extends Feature {
  lazy val extent = polygons.map(_.getExtent()).reduceLeft(_.combine(_))

  def getExtent() = extent
}
