package geotrellis.feature

import geotrellis._

import com.vividsolutions.jts.{ geom => jts }

class MultiPolygon[D](override val geom:jts.MultiPolygon, data:D) extends GeometryCollection(geom, data) {

  def flatten:List[Polygon[D]] =
    (0 until geom.getNumGeometries).map(
      i => new JtsPolygon(geom.getGeometryN(i).asInstanceOf[jts.Polygon],data)).toList

}

/// MultiPolygon implementation
object MultiPolygon {

  val factory = Feature.factory

  def apply[D](g: jts.Geometry, data: D): MultiPolygon[D] =
    JtsMultiPolygon(g.asInstanceOf[jts.MultiPolygon], data)
  def apply[D](g: jts.MultiPolygon, data: D): MultiPolygon[D] = JtsMultiPolygon(g, data)

  /**
   * Create a MultiPolygon using four nested lists.
   *
   * The first list represents each polygon.
   *
   * The second list represents each ring of the polygon, the first being
   * the exterior ring.
   *
   * The third list is a list of coordinates in the ring.
   *
   * The fourth list represents a single coordinate, which is two double values.
   *
   * @param coords  Nested sequence of polygon coordinates
   * @param data    The data of this feature
   */
  def apply[D](coords: Seq[Seq[Seq[Seq[Double]]]], data: D)(implicit dummy: DI, dummy2: DI): MultiPolygon[D] = {
    val polygons = coords.map(
      polygonCoords => {
        Polygon.createJtsPolygonFromSeqs(polygonCoords.head, polygonCoords.tail)
      }).toArray
    MultiPolygon(factory.createMultiPolygon(polygons), data)
  }

}

case class JtsMultiPolygon[D](g: jts.MultiPolygon, d: D) extends MultiPolygon(g,d)
