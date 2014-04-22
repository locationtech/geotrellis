package geotrellis.feature.srs

import geotrellis.feature._


abstract class Transformer[A <: SRS, B <: SRS] {
  def transform(x: Double, y: Double): (Double, Double)

  def transform(p: Point): Point = {
    val (x, y) = transform(p.x, p.y)
    Point(x, y)
  }

  def transform(mp: MultiPoint): MultiPoint =
    MultiPoint(mp.points.map{ transform(_) })

  def transform(ls: Line): Line =
    Line(ls.points.map{ transform(_) })

  def transform(ml: MultiLine): MultiLine = {
    MultiLine(ml.lines.map{ transform(_) })
  }

  def transform(p: Polygon): Polygon =
    Polygon(
      transform(p.exterior),
      p.holes.map{ transform(_) }.toSet //TODO - Set seems expensive why use it?
    )

  def transform(mp: MultiPolygon): MultiPolygon =
    MultiPolygon( mp.polygons.map{ transform(_) })

  def transform(gc: GeometryCollection): GeometryCollection =
    GeometryCollection(
      gc.points.map { transform(_) },
      gc.lines.map { transform(_) },
      gc.polygons.map { transform(_) },
      gc.multiPoints.map { transform(_) },
      gc.multiLines.map { transform(_) },
      gc.multiPolygons.map { transform(_) },
      gc.geometryCollections.map { transform(_) }
    )

  def transform(g: Geometry): Geometry =
    g match {
      case g: Point => transform(g)
      case g: MultiPoint => transform(g)
      case g: Line => transform(g)
      case g: MultiLine => transform(g)
      case g: Polygon => transform(g)
      case g: MultiPolygon => transform(g)
    }
}

object Transformer {
  def apply[A <: SRS, B <: SRS](f: (Double, Double) => (Double, Double)) = new Transformer[A, B] {
    override def transform(x: Double, y: Double) = f(x,y)
  }
}
