package geotrellis.feature

import GeomFactory._

import com.vividsolutions.jts.{geom => jts}
import scala.collection.mutable

/**
 * Builder for GeometryCollection.
 * This builder can accumulate from both geotrellis geometries and JTS geometries
 */
class GeometryCollectionBuilder {
  val points = mutable.ListBuffer[Point]()
  val lines = mutable.ListBuffer[Line]()
  val polygons = mutable.ListBuffer[Polygon]()
  val multiPoints = mutable.ListBuffer[MultiPoint]()
  val multiLines = mutable.ListBuffer[MultiLine]()
  val multiPolygons = mutable.ListBuffer[MultiPolygon]()
  val collections = mutable.ListBuffer[GeometryCollection]()

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

    new GeometryCollection(points, lines, polygons,
      multiPoints, multiLines, multiPolygons,
      collections, jtsGeom)
  }
}
