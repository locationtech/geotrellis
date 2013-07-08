package geotrellis.feature

import geotrellis._

import com.vividsolutions.jts.{ geom => jts }

class MultiLineString[D](override val geom:jts.MultiLineString, data:D) 
extends GeometryCollection(geom, data) {
  def flatten:List[LineString[D]] = 
    (0 until geom.getNumGeometries).map( 
      i => new JtsLineString(geom.getGeometryN(i).asInstanceOf[jts.LineString],data)).toList
}

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
