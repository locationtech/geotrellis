package geotrellis.feature

import scala.collection.mutable.ArrayBuffer
import scala.math.{min, max, round}

import geotrellis.geometry.grid.{GridPoint, GridLine, GridPolygon}
import geotrellis._


import com.vividsolutions.jts.geom.{GeometryFactory}
/*
import com.vividsolutions.jts.geom.{Coordinate => JtsCoordinate}
import com.vividsolutions.jts.geom.{Point      => JtsPoint}
import com.vividsolutions.jts.geom.{LineString => JtsLineString}
import com.vividsolutions.jts.geom.{LinearRing => JtsLinearRing}
import com.vividsolutions.jts.geom.{Polygon    => JtsPolygon}
import com.vividsolutions.jts.geom.{Geometry   => JtsGeometry}
*/

/**
 * Factory wraps GeometryFactory, which is used to build various JTS objects.
 */
object Factory extends UsesCoords {
  val f = new GeometryFactory()
}

