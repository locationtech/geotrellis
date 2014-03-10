package geotrellis.feature

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
      case gc:jts.GeometryCollection      => JtsGeometryCollection(gc, data)
      case _                              => JtsGeometry(p, data)
    }
  }
}
