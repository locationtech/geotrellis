package geotrellis.feature

import geotrellis._
import com.vividsolutions.jts.{ geom => jts }

class GeometryCollection[D](override val geom:jts.GeometryCollection, data:D) extends Geometry(geom,data)

case class JtsGeometryCollection[D](g: jts.GeometryCollection, d:D) extends GeometryCollection(g,d)

object GeometryCollection {
  def apply[D](geom:jts.GeometryCollection, d:D):JtsGeometryCollection[D] = JtsGeometryCollection(geom, d)
  def apply[D](geometries:Seq[jts.Geometry], d:D):JtsGeometryCollection[D] = {
    val geom = Feature.factory.createGeometryCollection(geometries.toArray)
    new JtsGeometryCollection(geom,d)
  }
}
