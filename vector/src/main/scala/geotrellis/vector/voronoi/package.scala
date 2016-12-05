package geotrellis.vector

import geotrellis.vector._

import com.vividsolutions.jts.{ geom => jts }


package object voronoi {

  implicit def linesToCollection(lines: Seq[Line]): jts.GeometryCollection  =
    new jts.GeometryCollection(lines.map({ line => line.jtsGeom }).toArray, GeomFactory.factory)

  implicit def linesToCollection(lines: Array[Line]): jts.GeometryCollection  =
    new jts.GeometryCollection(lines.map({ line => line.jtsGeom }), GeomFactory.factory)

  implicit def polygonsToCollection(polygons: Seq[Polygon]): jts.GeometryCollection =
    new jts.GeometryCollection(polygons.map({ poly => poly.jtsGeom }).toArray, GeomFactory.factory)

  implicit def polygonsToCollection(polygons: Array[Polygon]): jts.GeometryCollection =
    new jts.GeometryCollection(polygons.map({ poly => poly.jtsGeom }), GeomFactory.factory)
}
