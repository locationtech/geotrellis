package geotrellis.feature

import GeomFactory._

import com.vividsolutions.jts.{geom => jts}
import scala.collection.mutable

class GeometryCollection(val points: Set[Point],
                         val lines: Set[Line],
                         val polygons: Set[Polygon],
                         val geom: jts.GeometryCollection) {

  lazy val area: Double = geom.getArea
}

object GeometryCollection {

  implicit def jtsToGeometryCollection(gc: jts.GeometryCollection): GeometryCollection =
    apply(gc)

  def apply(points: Set[Point], lines: Set[Line], polygons: Set[Polygon]): GeometryCollection = {
    val geom = factory.createGeometryCollection((points ++ lines ++ polygons).map(_.geom).toArray)
    new GeometryCollection(points, lines, polygons, geom)
  }

  def apply(gc: jts.GeometryCollection) = {
    val (points, lines, polygons) = collectGeometries(gc)
    new GeometryCollection(points, lines, polygons, gc)
  }

  def unapply(gc: GeometryCollection) = Some((gc.points, gc.lines, gc.polygons))

  @inline final private 
  def collectGeometries(gc: jts.GeometryCollection): (Set[Point], Set[Line], Set[Polygon]) = {
    val points = mutable.Set[Point]()
    val lines = mutable.Set[Line]()
    val polygons = mutable.Set[Polygon]()

    val len = gc.getNumGeometries

    for(i <- 0 until len) {
      gc.getGeometryN(i) match {
        case p: jts.Point => points += p
        case mp: jts.MultiPoint => points ++= mp
        case l: jts.LineString => lines += l
        case ml: jts.MultiLineString => lines ++= ml
        case p: jts.Polygon => polygons += p
        case mp: jts.MultiPolygon => polygons ++= mp
        case gc: jts.GeometryCollection =>
          val (ps, ls, polys) = collectGeometries(gc)
          points ++= ps
          lines ++= ls
          polygons ++= polys
      }
    }

    (points.toSet, lines.toSet, polygons.toSet)
  }
}
