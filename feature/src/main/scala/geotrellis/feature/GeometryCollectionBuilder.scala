package geotrellis.feature

import com.vividsolutions.jts.geom
import GeomFactory._

import com.vividsolutions.jts.{geom => jts}
import scala.collection.mutable

/**
 * Builder for GeometryCollection.
 * jts types can be added using the implicit conversions provided in this package.
 */
class GeometryCollectionBuilder {
  val points = mutable.Set[Point]()
  val lines = mutable.Set[Line]()
  val polygons = mutable.Set[Polygon]()
  val multiPoints = mutable.Set[MultiPoint]()
  val multiLines = mutable.Set[MultiLine]()
  val multiPolygons = mutable.Set[MultiPolygon]()
  val collections = mutable.Set[GeometryCollection]()

  def add(geom: Geometry) = geom match {
    case p: Point => points += p
    case mp: MultiPoint => multiPoints += mp
    case l: Line => lines += l
    case ml: MultiLine => multiLines += ml
    case p: Polygon => polygons += p
    case mp: MultiPolygon => multiPolygons += mp
    case gc: GeometryCollection => collections += gc
  }
  def +=(geom: Geometry) = add(geom)

  def addAll(geoms: Traversable[Geometry]) =
    geoms.foreach(g=> add(g))
  def ++=(geoms: Traversable[Geometry]) =
    addAll(geoms)

  def addAll(geoms: Iterator[Geometry]) =
    geoms.foreach(g=> add(g))
  def ++=(geoms: Iterator[Geometry]) =
    addAll(geoms)

  def result(): GeometryCollection = {
    val jtsGeom = factory.createGeometryCollection(
      (
        points ++ lines ++ polygons ++
        multiPoints ++ multiLines ++ multiPolygons ++
        collections
      ).map(_.jtsGeom).toArray
    )

    new GeometryCollection(points.toSet, lines.toSet, polygons.toSet,
      multiPoints.toSet, multiLines.toSet, multiPolygons.toSet,
      collections.toSet, jtsGeom)
  }
}
