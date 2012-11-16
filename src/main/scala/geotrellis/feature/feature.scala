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
trait Feature[+G <: jts.Geometry, D] {

  // Corresponding FeatureSet to this kind of feature, e.g. Point[D] -> PointSet[D]
  type Set <: FeatureSet[D]

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
  
  /**
   * Returns a FeatureSet containing only this feature.
   */
  def toSet(): FeatureSet[D]
}
/// Dimensionality marker traits

trait Dim0 // zero-dimensional, e.g. point or multipoint
trait Dim1 // one-dimensional, e.g. LineString or MultiLineString
trait Dim2 // two-dimensional, e.g. Polygon or MultiPolygon

/// Simple feature traits

trait SingleGeometry[G <: jts.Geometry, D] extends Feature[G, D]

trait Point[D] extends SingleGeometry[jts.Point, D] with Dim0 {
  type Set = PointSet[D]
}

trait LineString[D] extends SingleGeometry[jts.LineString, D] with Dim1 {
  type Set = LineStringSet[D]
}

trait Polygon[D] extends SingleGeometry[jts.Polygon, D] with Dim2 {
  type Set = PolygonSet[D]
}

/// Multi feature traits

trait GeometryCollection[G <: jts.GeometryCollection, D] extends Feature[G, D]

trait MultiPoint[D] extends GeometryCollection[jts.MultiPoint, D] with Dim0 {
  type Set = MultiPointSet[D]
}

trait MultiLineString[D] extends GeometryCollection[jts.MultiLineString, D] with Dim1 {
  type Set = MultiLineStringSet[D]
}

trait MultiPolygon[D] extends GeometryCollection[jts.MultiPolygon, D] with Dim2 {
  type Set = MultiPolygonSet[D]
}

case class JtsGeometry[D](geom: jts.Geometry, data: D) extends Feature[jts.Geometry, D] {
  def toSet = sys.error("unimplemented")
}

/// Implementations

object Feature {
  /**
   * Returns a subclass of Feature given a geometry and data component.
   */
  def apply[D](p: jts.Geometry, data: D) = {
    p match {
      case point: jts.Point => JtsPoint(point, data)
      case polygon: jts.Polygon => JtsPolygon(polygon, data)
      case multiPoint: jts.MultiPoint => JtsMultiPoint(multiPoint, data)
      case multiPolygon: jts.MultiPolygon => JtsMultiPolygon(multiPolygon, data)
      case line: jts.LineString => JtsLineString(line,data)
      case multiLine: jts.MultiLineString => JtsMultiLineString(multiLine, data)
      case _ => JtsGeometry(p, data)
    }
  }
}


/// Point implementation 

/**
 * Point represents a simple (x,y) coordinate.
 */
object Point {
  def apply[A](x: Double, y: Double, z: A) = {
    val p = Factory.f.createPoint(new jts.Coordinate(x, y))
    JtsPoint(p, z)
  }

  def apply(x: Double, y: Double) = {
    JtsPoint(Factory.f.createPoint(new jts.Coordinate(x, y)), Unit)
  }
  
  def apply[D](p:jts.Point, data:D) = new JtsPoint(p,data) 
  
  def pointToGridCoords(p:Point[_], rasterExtent:RasterExtent) = {
    val geom = p.geom 
    rasterExtent.mapToGrid(geom.getX(), geom.getY())
  }
  
 def pointToRasterValue(p:Feature[jts.Point, _], raster:Raster) = {
    val re = raster.rasterExtent
    raster.get( re.mapXToGrid(p.geom.getX()), re.mapYToGrid(p.geom.getY()))
  }
}

/**
 * Point feature with a JTS Point internal representation.
 */
case class JtsPoint[D](geom: jts.Point, data: D) extends Point[D] {
  def toSet() = PointArray(Array(this))
}

/// Line implementation
object LineString {
  val factory = new jts.GeometryFactory()

  def apply[D](g: jts.Geometry, data: D): LineString[D] with Dim1 = 
    JtsLineString(g.asInstanceOf[jts.LineString],data)

  def apply[D](g:jts.LineString, data:D):LineString[D] with Dim1 = 
    JtsLineString(g,data)

  def apply[D](g:jts.LineString):LineString[Unit] with Dim1 = 
    JtsLineString(g,Unit)
    
  def apply[D](x0:Int,y0:Int,x1:Int,y1:Int, data:D):LineString[D] with Dim1 = {
    val g = factory.createLineString(Array( new jts.Coordinate(x0, y0), new jts.Coordinate(x1, y1)))
    JtsLineString(g, data)
  }
  
  def apply[D](x0:Double,y0:Double,x1:Double,y1:Double,data:D):LineString[D] with Dim1 = {
    val g = factory.createLineString(Array( new jts.Coordinate(x0, y0), new jts.Coordinate(x1, y1)))
    JtsLineString(g, data)
  }
}

case class JtsLineString[D](geom: jts.LineString, data: D) extends LineString[D] {
  def toSet = sys.error("unimplemented")
}

/// Polygon implementation
object Polygon {
  val factory = new jts.GeometryFactory()

  /**
   * Create a polgyon feature from a JTS Polygon object.
   */
  def apply[D](p: jts.Polygon, data: D): Polygon[D] with Dim2 = 
    JtsPolygon(p, data)

  /**
   * Create a polgyon feature from a JTS Geometry object.
   *
   * Only use when you are certain the Geometry object 
   * is a polygon.
   */ 
  def apply[D](g: jts.Geometry, data: D): Polygon[D] with Dim2 = 
    JtsPolygon(g.asInstanceOf[jts.Polygon], data)

  /**
   * Create a polygon using a list of tuples.
   *
   * This method is not very efficient -- use only for small polygons.
   */
  def apply[D](tpls:List[(Double,Double)], data:D): Polygon[D] with Dim2 =  {
    val jtsCoords = tpls.map { case (x,y) => new jts.Coordinate(x,y) }.toArray
    Polygon(jtsCoords, data) 
  }

  def apply[D](tpls:List[(Int,Int)], data:D) (implicit di:DummyImplicit): Polygon[D] with Dim2 = 
    Polygon(tpls.map { case (x,y) => (x.toDouble, y.toDouble) }, data)
  

  /**
   * Create a polygon using a one-dimensional array with alternating x and y values.
   */
  def apply[D](coords:Array[Double], data:D):Polygon[D] with Dim2 = {
    val jtsCoords = (0 until (coords.length / 2)).map { 
      (i) =>
        new jts.Coordinate(coords(i), coords(i+1))
    }.toArray
    Polygon(jtsCoords, data)
  }

  /**
   * Create a polygon with an array of JTS Coordinate objects.
   */
  def apply[D](coords:Array[jts.Coordinate], data:D):Polygon[D] with Dim2 = {
    val shell  = factory.createLinearRing(coords)
    val jts    = factory.createPolygon(shell, Array())
    JtsPolygon(jts, data)
  } 

   /**
   * Create a polygon using an array of rings, the first being the exterior ring.
   * 
   * Each ring array is an array of two element coordinate arrays, e.g. Array(x,y)
   */
  def apply[D](coords:Array[Array[Array[Double]]], data:D):Polygon[D] with Dim2 = {
    val exteriorRing = coords(0)
    val jtsCoords = (0 until exteriorRing.length).map {
      (i) => new jts.Coordinate(exteriorRing(i)(0), exteriorRing(i)(1))
    }.toArray
    Polygon(jtsCoords,data)
  }
}

case class JtsPolygon[D](geom: jts.Polygon, data: D) extends Polygon[D] {
  def toSet = sys.error("unimplemented")
}

/// MultiPoint implementation
object MultiPoint {
  def apply[D](g: jts.MultiPoint, data: D) = JtsMultiPoint(g, data)
}

case class JtsMultiPoint[D](geom: jts.MultiPoint, data: D) extends MultiPoint[D] {
  def toSet = sys.error("unimplemented")
}

/// MultiLineString implementation
object MultiLineString {
  def apply[D](g: jts.MultiLineString, data: D) = JtsMultiLineString(g, data)
}

case class JtsMultiLineString[D](geom: jts.MultiLineString, data: D) extends MultiLineString[D] {
  def toSet = sys.error("unimplemented")
}

/// MultiPolygon implementation
object MultiPolygon {
  def apply[D](g: jts.Geometry, data: D): MultiPolygon[D] = JtsMultiPolygon(g.asInstanceOf[jts.MultiPolygon], data)
  def apply[D](g: jts.MultiPolygon, data: D): MultiPolygon[D] = JtsMultiPolygon(g, data)
}

case class JtsMultiPolygon[D](geom: jts.MultiPolygon, data: D) extends MultiPolygon[D] {
  def toSet = sys.error("unimplemented")
}

/**
 * Turn tuples into JtsCoordinates.
 */
trait UsesCoords {
  def makeCoord(x: Double, y: Double) = { new jts.Coordinate(x, y) }

  def makeCoords(tpls: Array[(Double, Double)]) = {
    tpls.map { pt => makeCoord(pt._1, pt._2) }.toArray
  }
}


trait FeatureSet[D] {
  type Geom <: jts.Geometry
  type Feat // = ({ type Feat = Feature[Geom, D] })#Feat
  type Data = D

  def length: Int
  def lengthLong: Long

  def chunk(n: Int): List[FeatureSet[D]]

  def foreach(f: Feat => Unit): Unit
  def foldLeft[A](initA: A)(f: (A, Feat) => A): A = {
    var state = initA
    foreach((feature) => state = f(state, feature))
    state
  }
  def filter(f: Feat => Boolean): FeatureSet[D]

  def map[F <: Feature[_, _], FS <: FeatureSet[_]](f: Feat => F)(implicit b: SetBuilder[F, FS], n: Manifest[F]): FS
}

trait SetBuilder[-F, +FS] {
  type Set = FS
  def build[G <: F](array: Array[G]): FS
}

object SetBuilder {
  implicit def buildPointSet[D] = new SetBuilder[Point[D], PointSet[D]] {
    def build[G <: Point[D]] (array: Array[G]) = PointArray(array.asInstanceOf[Array[Point[D]]] )
  }
}

trait PointSet[D] extends FeatureSet[D] {
  type Geom = jts.Point
  type Feat = Point[D]
}

object PointSet {
  def apply[D] (points:List[Point[D]]) = PointArray(points.toArray)
  
  def apply[D:Manifest](xs: Array[Double], ys: Array[Double], ds: Array[D]) = 
    new UnboxedPointSet(xs, ys, ds)
}

case class PointArray[D](points: Array[Point[D]]) extends PointSet[D] {
  def length = points.length
  def lengthLong = length.toLong

  def chunk(n: Int) = {
    var arrays: List[PointArray[D]] = Nil
    val full_chunks = length / n
    val remainder = length % n
    for (j <- 0 until full_chunks) {
      val a = Array.ofDim[Point[D]](n)
      System.arraycopy(points, j * n, a, 0, n)
      arrays = PointArray(a) :: arrays
      //arrays = PointArray(a.toList) :: arrays
    }
    if (remainder != 0) {
      val a = Array.ofDim[Feat](remainder)
      System.arraycopy(points, full_chunks * n, a, 0, remainder)
      //arrays = PointArray(a.toList) :: arrays
      arrays = PointArray(a) :: arrays
    }
    arrays
  }

  def filter(f: Feat => Boolean) = PointArray(points.filter(f))
  def foreach(f: Feat => Unit) = points.foreach(f)

  def map[F <: Feature[_, _], FS <: FeatureSet[_]](f: Feat => F)(implicit b: SetBuilder[F, FS], n: Manifest[F]) = {
    b.build(points.map(f))
  }
}

case class UnboxedPointSet[D: Manifest](xs: Array[Double], ys: Array[Double], ds: Array[D]) extends PointSet[D] {
  def length = xs.length
  def lengthLong = xs.length.toLong
  def chunk(n: Int) = {
    var arrays: List[UnboxedPointSet[D]] = Nil
    val full_chunks = length / n
    val remainder = length % n
    var i = 0
    for (j <- 0 until full_chunks) {
      val xs2 = Array.ofDim[Double](n)
      val ys2 = Array.ofDim[Double](n)
      val ds2 = Array.ofDim[D](n)
      System.arraycopy(xs, j * n, xs2, 0, n)
      System.arraycopy(ys, j * n, ys2, 0, n)
      System.arraycopy(ds, j * n, ds2, 0, n)
      arrays = UnboxedPointSet(xs2, ys2, ds2) :: arrays
    }
    if (remainder != 0) {
      val a = Array.ofDim[Point[D]](remainder)
      val xs2 = Array.ofDim[Double](n)
      val ys2 = Array.ofDim[Double](n)
      val ds2 = Array.ofDim[D](n)
      System.arraycopy(xs, full_chunks * n, xs2, 0, remainder)
      System.arraycopy(ys, full_chunks * n, ys2, 0, remainder)
      System.arraycopy(ds, full_chunks * n, ds2, 0, remainder)
      arrays = UnboxedPointSet(xs2, ys2, ds2) :: arrays
    }
    arrays
  }

  def foreach(f: Feat => Unit) {
    var i = 0
    val length = xs.length
    while (i < length) {
      f(Point(xs(i), ys(i), ds(i)))
      i += 1
    }
  }

  def filter(f: Feat => Boolean) = {
    val xs2 = new ArrayBuffer[Double]()
    val ys2 = new ArrayBuffer[Double]()
    val ds2 = new ArrayBuffer[D]()

    var i = 0
    val length = xs.length
    while (i < length) {
      val x = xs(i)
      val y = ys(i)
      val d = ds(i)
      if (f(Point(x, y, d))) {
        xs2.append(x)
        ys2.append(y)
        ds2.append(d)
      }
      i += 1
    }
    UnboxedPointSet(xs2.toArray, ys2.toArray, ds2.toArray)
  }

  def map[F <: Feature[_, _], FS <: FeatureSet[_]](f: Feat => F)(implicit b: SetBuilder[F, FS], n: Manifest[F]) = {
    val array = (0 until xs.length).map {
      i => f(Point(xs(i), ys(i), ds(i)))
    }.toArray
    b.build(array)
  }
}

trait PolygonSet[D] extends FeatureSet[D] {
  type Geom = jts.Polygon
}

trait LineStringSet[D] extends FeatureSet[D] {
  type Geom = jts.LineString
}

trait MultiPointSet[D] extends FeatureSet[D] {
  type Geom = jts.MultiPoint
}

trait MultiLineStringSet[D] extends FeatureSet[D] {
  type Geom = jts.MultiLineString
}

trait MultiPolygonSet[D] extends FeatureSet[D] {
  type Geom = jts.MultiPolygon
}
