package geotrellis.feature

import geotrellis._

import com.vividsolutions.jts.{ geom => jts }

class Geometry[D] (val geom:jts.Geometry, val data:D) extends Feature[jts.Geometry, D]

class SingleGeometry[D] (override val geom:jts.Geometry, data:D) extends Geometry(geom, data)

class GeometryCollection[D](override val geom:jts.GeometryCollection, data:D) extends Geometry(geom,data)

case class JtsGeometry[D](g: jts.Geometry, d: D) extends Geometry(g,d)

/**
 * Turn tuples into JtsCoordinates.
 */
trait UsesCoords {
  def makeCoord(x: Double, y: Double) = { new jts.Coordinate(x, y) }

  def makeCoords(tpls: Array[(Double, Double)]) = {
    tpls.map { pt => makeCoord(pt._1, pt._2) }.toArray
  }
}

