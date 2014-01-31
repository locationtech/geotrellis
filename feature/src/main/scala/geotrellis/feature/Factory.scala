package geotrellis.feature

import com.vividsolutions.jts.geom

private[feature] object GeomFactory {
  val factory = new geom.GeometryFactory()
}
