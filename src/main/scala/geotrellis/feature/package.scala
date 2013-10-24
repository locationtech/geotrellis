package geotrellis
import com.vividsolutions.jts.{ geom => jts }

package object feature {
  implicit class WrappedJtsGeometry(geom:jts.Geometry) {
    private def flattenToPolygon(g:jts.Geometry):List[jts.Polygon] =
      g match {
        case g: jts.GeometryCollection =>
          (0 until g.getNumGeometries).flatMap { i =>
            flattenToPolygon(g.getGeometryN(i))
          }.toList
        case l: jts.LineString => List()
        case p: jts.Point => List()
        case p: jts.Polygon => List(p)
      }

    def asPolygonSet() = flattenToPolygon(geom)
  }
}
