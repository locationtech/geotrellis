package geotrellis.feature

import com.vividsolutions.jts.{geom=>jts}
import GeomFactory._

trait GeometrySet extends Geometry {
  def &(p:Point) = intersection(p)
  def intersection(p:Point):PointIntersectionResult =
    p.intersection(this)

  def &(ps:PointSet) = intersection(ps)
  def intersection(ps:PointSet):PointSetIntersectionResult =
    geom.intersection(ps.geom)

}
