package geotrellis.feature

import GeomFactory._

import com.vividsolutions.jts.{geom=>jts}
import scala.collection.mutable

case class GeometryCollection(points:Set[Point],lines:Set[Line],polygons:Set[Polygon],gc:jts.GeometryCollection) {
  val geom = factory.createGeometryCollection((points ++ lines ++ polygons).map(_.geom).toArray)
}

object GeometryCollection {
  def apply(points:Set[Point],lines:Set[Line],polygons:Set[Polygon]):GeometryCollection = {
    val geom = factory.createGeometryCollection((points ++ lines ++ polygons).map(_.geom).toArray)
    GeometryCollection(points,lines,polygons,geom)
  }

  implicit def jtsToGeometryCollection(gc:jts.GeometryCollection):GeometryCollection = {
    val (points,lines,polygons) = collectGeometries(gc)
    GeometryCollection(points,lines,polygons,gc)
  }

  @inline final private 
  def collectGeometries(gc:jts.GeometryCollection):(Set[Point],Set[Line],Set[Polygon]) = {
    val points = mutable.Set[Point]()
    val lines = mutable.Set[Line]()
    val polygons = mutable.Set[Polygon]()

    val len = gc.getNumGeometries
    for(i <- 0 until len) {
      gc.getGeometryN(i) match {
        case p:jts.Point => points += p
        case mp:jts.MultiPoint => points ++= mp
        case l:jts.LineString => lines += l
        case ml:jts.MultiLineString => lines ++= ml
        case p:jts.Polygon => polygons += p
        case mp:jts.MultiPolygon => polygons ++= mp
        case gc:jts.GeometryCollection =>
          val (ps,ls,polys) = collectGeometries(gc)
          points ++= ps
          lines ++= ls
          polygons ++= polys
      }
    }
    (points.toSet,lines.toSet,polygons.toSet)
  }
}
