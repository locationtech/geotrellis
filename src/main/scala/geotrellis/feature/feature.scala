package geotrellis.feature

import scala.collection.mutable.ArrayBuffer
import scala.math.{ min, max, round }

import com.vividsolutions.jts.{ geom => jts }
import geotrellis._

/**
 * Represents a feature on a map.
 *
 * A feature has two components: a geometry, representing where it is,
 * and a data component, representing what it is.
 *
 * The geometry component can be returned as a JTS geometry object.
 *
 * The data component is generic.
 *
 */
trait Feature[+G <: jts.Geometry, D] extends Serializable {

  /**
   * Returns geometry as a JTS Geometry object.
   */
  def geom(): G

  /**
   * Returns the data component.
   */
  def data(): D

  /**
   * Returns a new Feature given a function that takes a Geometry object
   * and returns a new geometry object.  The data component remains the same.
   */
  def mapGeom[H <: jts.Geometry](f: G => H) = Feature(f(geom), data)

}
/// Dimensionality marker traits

trait Dim0 // zero-dimensional, e.g. point or multipoint
trait Dim1 // one-dimensional, e.g. LineString or MultiLineString
trait Dim2 // two-dimensional, e.g. Polygon or MultiPolygon

/// Simple feature traits

class Geometry[D] (val geom:jts.Geometry, val data:D) extends Feature[jts.Geometry, D]

class SingleGeometry[D] (override val geom:jts.Geometry, data:D) extends Geometry(geom, data)

class Point[D] (override val geom:jts.Point, data:D) extends SingleGeometry(geom,data)

class LineString[D] (override val geom:jts.LineString, data:D) extends SingleGeometry(geom,data)

class Polygon[D] (override val geom:jts.Polygon, data:D) extends SingleGeometry(geom,data)

/// Multi feature traits

class GeometryCollection[D](override val geom:jts.GeometryCollection, data:D) extends Geometry(geom,data)

class MultiPoint[D](override val geom:jts.MultiPoint, data:D) extends GeometryCollection(geom,data) {

  def flatten:List[Point[D]] =
    (0 until geom.getNumGeometries).map(
      i => new JtsPoint(geom.getGeometryN(i).asInstanceOf[jts.Point],data)).toList

}

class MultiLineString[D](override val geom:jts.MultiLineString, data:D) extends GeometryCollection(geom, data) {
  def flatten:List[LineString[D]] = 
    (0 until geom.getNumGeometries).map( 
      i => new JtsLineString(geom.getGeometryN(i).asInstanceOf[jts.LineString],data)).toList
}

class MultiPolygon[D](override val geom:jts.MultiPolygon, data:D) extends GeometryCollection(geom, data) {

  def flatten:List[Polygon[D]] =
    (0 until geom.getNumGeometries).map(
      i => new JtsPolygon(geom.getGeometryN(i).asInstanceOf[jts.Polygon],data)).toList

}

case class JtsGeometry[D](g: jts.Geometry, d: D) extends Geometry(g,d)

/// Implementations

object Feature {

  val factory = new jts.GeometryFactory()

  /**
   * Returns a subclass of Feature given a geometry and data component.
   */
  def apply[D](p: jts.Geometry, data: D) = {
    p match {
      case point: jts.Point               => JtsPoint(point, data)
      case polygon: jts.Polygon           => JtsPolygon(polygon, data)
      case multiPoint: jts.MultiPoint     => JtsMultiPoint(multiPoint, data)
      case multiPolygon: jts.MultiPolygon => JtsMultiPolygon(multiPolygon, data)
      case line: jts.LineString           => JtsLineString(line, data)
      case multiLine: jts.MultiLineString => JtsMultiLineString(multiLine, data)
      case _                              => JtsGeometry(p, data)
    }
  }
}

/// Point implementation 

/**
 * Point represents a simple (x,y) coordinate.
 *
 *
 */
object Point {
  def factory = Feature.factory

  /**
   * Create a point feature.
   *
   * @param   x   x coordinate
   * @param   y   y coordinate
   * @param   d   Data of this feature
   */
  def apply[D](x: Double, y: Double, data: D) = {
    val p = factory.createPoint(new jts.Coordinate(x, y))
    JtsPoint(p, data)
  }

  /**
   * Create a point feature without data.
   *
   * @param   x   x coordinate
   * @param   y   y coordinate
   */
  def apply(x: Double, y: Double) = {
    JtsPoint(factory.createPoint(new jts.Coordinate(x, y)), None)
  } 
  /**
   * Create a point feature from a JTS point instance.
   *
   * @param p     JTS Point instance
   * @param data  Data of this feature.
   */
  def apply[D](p: jts.Point, data: D) = new JtsPoint(p, data)

  /**
   * Calculate row and column of this point in given raster extent.
   *
   * @param p             Point feature
   * @param rasterExtent  Raster extent to place feature within
   */
  def pointToGridCoords(p: Point[_], rasterExtent: RasterExtent) = {
    val geom = p.geom
    rasterExtent.mapToGrid(geom.getX(), geom.getY())
  }

  /**
   * Get value from raster at cell corresponding to given point.
   *
   * @param p       Point feature
   * @param raster  Raster to get value from
   */
  def pointToRasterValue(p: Feature[jts.Point, _], raster: Raster) = {
    val re = raster.rasterExtent
    raster.get(re.mapXToGrid(p.geom.getX()), re.mapYToGrid(p.geom.getY()))
  }
}

/**
 * Point feature with a JTS Point internal representation.
 */
case class JtsPoint[D](g: jts.Point, d: D) extends Point(g,d)

/// Line implementation
object LineString {
  val factory = Feature.factory

  /**
   * Create a LineString (aka a line) feature.
   *
   * @param   g     jts.Geometry object
   * @param   data  Data of this feature
   */
  def apply[D](g: jts.Geometry, data: D): LineString[D] =
    JtsLineString(g.asInstanceOf[jts.LineString], data)

  /**
   * Create a LineString (aka a line) feature.
   *
   * @param   g     JTS LineString object
   * @param   data  Data of this feature
   */
  def apply[D](g: jts.LineString, data: D): LineString[D] =
    JtsLineString(g, data)

  /**
   * Create a LineString (aka a line) given x and y coordinates, as integers.
   *
   * @param x0  x coordinate of first point
   * @param y0  y coordinate of first point
   * @param x1  x coordinate of second point
   * @param y1  y coordinate of second point
   * @param data  Data value of this feature
   */
  def apply[D](x0: Double, y0: Double, x1: Double, y1: Double, data: D): LineString[D] = {
    val g = factory.createLineString(Array(new jts.Coordinate(x0, y0), new jts.Coordinate(x1, y1)))
    JtsLineString(g, data)
  }

  /**
   * Create a LineString (aka a line) given x and y coordinates, as integers.
   *
   * @param x0  x coordinate of first point
   * @param y0  y coordinate of first point
   * @param x1  x coordinate of second point
   * @param y1  y coordinate of second point
   * @param data  Data value of this feature
   */
  def apply[D](x0: Int, y0: Int, x1: Int, y1: Int, data: D): LineString[D] = {
    val g = factory.createLineString(Array(new jts.Coordinate(x0, y0), new jts.Coordinate(x1, y1)))
    JtsLineString(g, data)
  }

  /**
   * Create a LineString (aka a line) given x and y coordinates, as integers.
   *
   * @param tpls  Seq of (x,y) tuples
   * @param data  Data value of this feature
   */
  def apply[D](tpls: Seq[(Int, Int)], data: D): LineString[D] = {
    val coords = tpls.map { t => new jts.Coordinate(t._1, t._2) }.toArray
    val g = factory.createLineString(coords)
    JtsLineString(g, data)
  }

  /**
   * Create a LineString (aka a line) given x and y coordinates, as integers.
   *
   * @param tpls  Seq of (x,y) tuples
   * @param data  Data value of this feature
   */
  def apply[D](tpls: Seq[(Double, Double)], data: D)(implicit d:DummyImplicit): LineString[D] = {
    val coords = tpls.map { t => new jts.Coordinate(t._1, t._2) }.toArray
    val g = factory.createLineString(coords)
    JtsLineString(g, data)
  }
}

/**
 * Implementation of LineString feature with underlying jts instance.
 */
case class JtsLineString[D](g: jts.LineString, d: D) extends LineString(g,d)

/// Polygon implementation
object Polygon {
  val factory = Feature.factory
  /**
   * Create a polgyon feature from a JTS Polygon object.
   *
   * @param   p     JTS Polygon object
   * @param   data  The data of this feature
   */
  def apply[D](p: jts.Polygon, data: D): Polygon[D] =
    JtsPolygon(p, data)

  /**
   * Create a polygon using a list of tuples.
   *
   * This method is not very efficient -- use only for small polygons.
   *
   * @param tpls  Seq of (x,y) tuples
   * @param data  The data of this feature
   */
  def apply[D](tpls: Seq[(Double, Double)], data: D): Polygon[D] = {
    val jtsCoords = tpls.map { case (x, y) => new jts.Coordinate(x, y) }.toArray
    Polygon(jtsCoords, data)
  }

  def apply[D](tpls: Seq[(Int, Int)], data: D)(implicit di: DummyImplicit): Polygon[D] =
    Polygon(tpls.map { case (x, y) => (x.toDouble, y.toDouble) }, data)

  /**
   * Create a polygon using a one-dimensional array with alternating x and y values.
   *
   * @param coords  Array of alternating x and y values
   * @param data    The data of this feature
   */
  def apply[D](coords: Array[Double], data: D): Polygon[D] = {
    val jtsCoords = (0 until (coords.length / 2)).map {
      (i) =>
        new jts.Coordinate(coords(i), coords(i + 1))
    }.toArray
    Polygon(jtsCoords, data)
  }

  /**
   * Create a polygon with an array of JTS Coordinate objects.
   *
   * @param coords  Coordinates of the polygon exterior
   * @param data    The data of this feature
   */
  def apply[D](coords: Array[jts.Coordinate], data: D): Polygon[D] = {
    val shell = factory.createLinearRing(coords)
    val jts = factory.createPolygon(shell, Array())
    JtsPolygon(jts, data)
  }

  /**
   * Create a polygon with arrays of JTS coordinate objects.
   *
   * @param exterior  Coordinates of the exterior shell
   * @param holes     Interior holes represented by array of coordinate arrays
   * @param data      The data of this feature
   */
  def apply[D](exterior: Array[jts.Coordinate], holes: Array[Array[jts.Coordinate]], data: D): Polygon[D] = {
    val shellRing = factory.createLinearRing(exterior)
    val holeRings = holes.map( factory.createLinearRing(_) ).toArray
    val jts = factory.createPolygon(shellRing, holeRings)
    JtsPolygon(createJtsPolygon(exterior, holes), data)
  }

  protected[geotrellis] def createJtsPolygon(exterior: Array[jts.Coordinate], holes: Array[Array[jts.Coordinate]]) = {
    val shellRing = factory.createLinearRing(exterior)
    val holeRings = holes.map(factory.createLinearRing(_)).toArray
    factory.createPolygon(shellRing, holeRings)
  }

  protected[geotrellis] def createJtsPolygonFromArrays(exterior: Array[Array[Double]], holes: Array[Array[Array[Double]]]) = {
    val shellRing = (0 until exterior.length).map {
      (i) => new jts.Coordinate(exterior(i)(0), exterior(i)(1))
    }.toArray
    val holeRings = holes.map(
      ring => ring.map(
        coordArray => {
          new jts.Coordinate(coordArray(0), coordArray(1))
        }))
    val polygon = createJtsPolygon(shellRing, holeRings)
    polygon
  }

  protected[geotrellis] def createJtsPolygonFromSeqs(exterior: Seq[Seq[Double]], holes: Seq[Seq[Seq[Double]]]) = {
    val shellRing = (0 until exterior.length).map {
      (i) => new jts.Coordinate(exterior(i)(0), exterior(i)(1))
    }.toArray
    val holeRings = holes.map(
      ring => ring.map(
        coordArray => {
          new jts.Coordinate(coordArray(0), coordArray(1))
        }).toArray).toArray
    createJtsPolygon(shellRing, holeRings)
  }

  /**
   * Create a polygon using an array of rings, the first being the exterior ring.
   *
   * Each ring array is an array of two element coordinate arrays, e.g. Array(x,y)
   */
  def apply[D](coords: Array[Array[Array[Double]]], data: D): Polygon[D] =
    Polygon(createJtsPolygonFromArrays(coords.head, coords.tail), data)

  /**
   * Create a polygon using an array of rings, the first being the exterior ring.
   *
   * Each ring array is an array of two element coordinate arrays, e.g. Array(1.0, 2.0)
   *
   * The top level list is the list of rings, the first inner list is a list of coordinates,
   * and each inner list has two elements, x and y.
   *
   * @param coords   A list of polygon rings, represented as a list of two element lists.
   * @param data     The data for this feature.
   */
  def apply[D](coords: Seq[Seq[Seq[Double]]], data: D)(implicit dummy: DI, dummy2: DI): Polygon[D] = {
    val exterior = coords.head
    val shellRing = (0 until exterior.length).map {
      (i) => new jts.Coordinate(exterior(i)(0), exterior(i)(1))
    }.toArray
    val holeRings = coords.tail.map(
      ring => ring.map(
        coordArray => {
          new jts.Coordinate(coordArray(0), coordArray(1))
        }).toArray).toArray

    Polygon(shellRing, holeRings, data)
  }

  /**
   * Create a polgyon feature from a JTS Geometry object.
   *
   * Beware: Only use when you are certain the Geometry object
   * is a polygon.
   *
   * @param g   JTS Geometry
   * @param d   The data of this feature
   */
  def apply[D](g: jts.Geometry, data: D): Polygon[D] =
    JtsPolygon(g.asInstanceOf[jts.Polygon], data)

}

case class JtsPolygon[D](g: jts.Polygon, d: D) extends Polygon(g, d)

/// MultiPoint implementation
object MultiPoint {
  val factory = Feature.factory

  def apply[D](g: jts.MultiPoint, data: D):JtsMultiPoint[D] = JtsMultiPoint(g, data)
  /**
   * Create a MultiPoint feature with sequences of coordinate values.
   *
   * The coordinate values are represented as a sequence of coordinates, each
   * represented as a sequence of two double values (x and y).
   *
   * @param coords    Sequence of x and y sequences
   * @param data      The data of this feature
   */
  def apply[D](coords: Seq[Seq[Double]], data: D):JtsMultiPoint[D] = {
    val jtsMP = factory.createMultiPoint (
      coords.map ( coord => { 
        new jts.Coordinate(coord(0),coord(1))}).toArray
    )
    MultiPoint(jtsMP, data)    
  }
}

case class JtsMultiPoint[D](g: jts.MultiPoint, d: D) extends MultiPoint[D](g, d)

/// MultiLineString implementation
/**
 * A MultiLineString feature is a set of lines with associated data.
 */
object MultiLineString {
  val factory = Feature.factory 

  def apply[D](g: jts.MultiLineString, data: D) = JtsMultiLineString(g, data)

  /**
   * Create a MultiLineString feature with sequences of coordinate values.
   *
   * A MultiLineString feature is *
   * The coordinate values are represented as a sequence of coordinates, each
   * represented as a sequence of two double values (x and y).
   *
   * @param coords    Sequence of x and y sequences
   * @param data      The data of this feature
   */
  def apply[D](multiLineCoords: Seq[Seq[Seq[Double]]], data: D):MultiLineString[D] = {
    val jtsLines = multiLineCoords.map( coords => {
      val coordArray = Array (
          new jts.Coordinate(coords(0)(0), coords(0)(1)),
          new jts.Coordinate(coords(1)(0), coords(1)(1))
      )
      factory.createLineString (coordArray)
    }).toArray
    MultiLineString(factory.createMultiLineString(jtsLines), data) 
  }
}

case class JtsMultiLineString[D](g: jts.MultiLineString, d: D) extends MultiLineString(g,d)

/// MultiPolygon implementation
object MultiPolygon {

  val factory = Feature.factory

  def apply[D](g: jts.Geometry, data: D): MultiPolygon[D] =
    JtsMultiPolygon(g.asInstanceOf[jts.MultiPolygon], data)
  def apply[D](g: jts.MultiPolygon, data: D): MultiPolygon[D] = JtsMultiPolygon(g, data)

  /**
   * Create a MultiPolygon using four nested lists.
   *
   * The first list represents each polygon.
   *
   * The second list represents each ring of the polygon, the first being
   * the exterior ring.
   *
   * The third list is a list of coordinates in the ring.
   *
   * The fourth list represents a single coordinate, which is two double values.
   *
   * @param coords  Nested sequence of polygon coordinates
   * @param data    The data of this feature
   */
  def apply[D](coords: Seq[Seq[Seq[Seq[Double]]]], data: D)(implicit dummy: DI, dummy2: DI): MultiPolygon[D] = {
    val polygons = coords.map(
      polygonCoords => {
        Polygon.createJtsPolygonFromSeqs(polygonCoords.head, polygonCoords.tail)
      }).toArray
    MultiPolygon(factory.createMultiPolygon(polygons), data)
  }

}

case class JtsMultiPolygon[D](g: jts.MultiPolygon, d: D) extends MultiPolygon(g,d)

/**
 * Turn tuples into JtsCoordinates.
 */
trait UsesCoords {
  def makeCoord(x: Double, y: Double) = { new jts.Coordinate(x, y) }

  def makeCoords(tpls: Array[(Double, Double)]) = {
    tpls.map { pt => makeCoord(pt._1, pt._2) }.toArray
  }
}


