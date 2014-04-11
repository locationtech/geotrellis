package geotrellis.feature

import com.vividsolutions.jts.geom
import GeomFactory._

import com.vividsolutions.jts.{geom => jts}
import scala.collection.mutable

/**
 * Builder for GeometryCollection.
 * This builder can accumulate from both geotrellis geometries and JTS geometries
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


  def add(geom: jts.Geometry) = geom match {
    //implicit conversions are happening here
    case p: jts.Point => points += p
    case mp: jts.MultiPoint => multiPoints += mp
    case l: jts.LineString => lines += l
    case ml: jts.MultiLineString => multiLines += ml
    case p: jts.Polygon => polygons += p
    case mp: jts.MultiPolygon => multiPolygons += mp
    case gc: jts.GeometryCollection => collections += gc
  }
  def +=(geom: jts.Geometry) = add(geom)

  def addAll(geoms: Traversable[jts.Geometry])(implicit d: DummyImplicit) =
    geoms.foreach(g=> add(g))
  def ++=(geoms: Traversable[jts.Geometry])(implicit d: DummyImplicit) =
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
