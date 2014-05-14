package geotrellis.feature

import geotrellis.proj4
import geotrellis.proj4.CRS

package object reproject {
  object Reproject {
    def apply(t: (Double, Double), src: CRS, dest: CRS): (Double, Double) =
      proj4.Reproject(t._1, t._2, src, dest)

    def apply(p: Point, src: CRS, dest: CRS): Point = {
      val (x,y) = apply((p.x, p.y), src, dest)
      Point(x,y)
    }

    def apply[D](pf:PointFeature[D], src: CRS, dest: CRS): PointFeature[D] =
      PointFeature[D](apply(pf.geom, src, dest), pf.data)

    def apply(l: Line, src: CRS, dest: CRS): Line =
      Line(l.points.map{ apply(_, src, dest) })

    def apply[D](lf: LineFeature[D], src: CRS, dest: CRS): LineFeature[D] =
      LineFeature(apply(lf.geom, src, dest), lf.data)

    def apply(p: Polygon, src: CRS, dest: CRS): Polygon =
      Polygon(
        apply(p.exterior, src, dest),
        p.holes.map{ apply(_, src, dest) }
      )

    def apply[D](pf: PolygonFeature[D], src: CRS, dest: CRS): PolygonFeature[D] =
      PolygonFeature(apply(pf.geom, src, dest), pf.data)

    def apply(mp: MultiPoint, src: CRS, dest: CRS): MultiPoint =
      MultiPoint(mp.points.map(apply(_, src, dest)))

    def apply[D](mpf: MultiPointFeature[D], src: CRS, dest: CRS): MultiPointFeature[D] =
      MultiPointFeature(apply(mpf.geom, src, dest), mpf.data)

    def apply(ml: MultiLine, src: CRS, dest: CRS): MultiLine =
      MultiLine(ml.lines.map(apply(_, src, dest)))

    def apply[D](mlf: MultiLineFeature[D], src: CRS, dest: CRS): MultiLineFeature[D] =
      MultiLineFeature(apply(mlf, src, dest), mlf.data)

    def apply(mp: MultiPolygon, src: CRS, dest: CRS): MultiPolygon =
      MultiPolygon(mp.polygons.map(apply(_, src, dest)))

    def apply[D](mpf: MultiPolygonFeature[D], src: CRS, dest: CRS): MultiPolygonFeature[D] =
      MultiPolygonFeature(apply(mpf.geom, src, dest), mpf.data)

    def apply(gc: GeometryCollection, src: CRS, dest: CRS): GeometryCollection = 
      GeometryCollection(
        gc.points.map{ apply(_, src, dest) },
        gc.lines.map{ apply(_, src, dest) },
        gc.polygons.map{ apply(_, src, dest) },
        gc.multiPoints.map{ apply(_, src, dest) },
        gc.multiLines.map{ apply(_, src, dest) },
        gc.multiPolygons.map{ apply(_, src, dest) },
        gc.geometryCollections.map{ apply(_, src, dest) }
      )

    def apply[D](gcf: GeometryCollectionFeature[D], src: CRS, dest: CRS): GeometryCollectionFeature[D] =
      GeometryCollectionFeature(apply(gcf.geom, src, dest), gcf.data)

    def apply(g: Geometry, src: CRS, dest: CRS): Geometry =
      g match {
        case p: Point => apply(p, src, dest)
        case l: Line => apply(l, src, dest)
        case p: Polygon => apply(p, src, dest)
        case mp: MultiPoint => apply(mp, src, dest)
        case ml: MultiLine => apply(ml, src, dest)
        case mp: MultiPolygon => apply(mp, src, dest)
        case gc: GeometryCollection => apply(gc, src, dest)
      }

    def apply[D](f: Feature[D], src: CRS, dest: CRS): Feature[D] =
      f match {
        case pf: PointFeature[D] => apply(pf, src, dest)
        case lf: LineFeature[D] => apply(lf, src, dest)
        case pf: PolygonFeature[D] => apply(pf, src, dest)
        case mpf: MultiPointFeature[D] => apply(mpf, src, dest)
        case mlf: MultiLineFeature[D] => apply(mlf, src, dest)
        case mpf: MultiPolygonFeature[D] => apply(mpf, src, dest)
        case gcf: GeometryCollectionFeature[D] => apply(gcf, src, dest)
      }
  }

  implicit class RerpojectTuple(t: (Double,Double)) { def reproject(src: CRS, dest: CRS): (Double, Double) = Reproject(t, src, dest) }
  implicit class ReprojectPoint(p: Point) { def reproject(src: CRS, dest: CRS): Point = Reproject(p, src, dest) }
  implicit class ReprojectPointFeature[D](pf: PointFeature[D]) { def reproject(src: CRS, dest: CRS): PointFeature[D] = Reproject(pf, src, dest) }
  implicit class ReprojectLine(l: Line) { def reproject(src: CRS, dest: CRS): Line = Reproject(l, src, dest) }
  implicit class ReprojectLineFeature[D](lf: LineFeature[D]) { def reproject(src: CRS, dest: CRS): LineFeature[D] = Reproject(lf, src, dest) }
  implicit class ReprojectPolygon(p: Polygon) { def reproject(src: CRS, dest: CRS): Polygon = Reproject(p, src, dest) }
  implicit class ReprojectPolygonFeature[D](pf: PolygonFeature[D]) { def reproject(src: CRS, dest: CRS): PolygonFeature[D] = Reproject(pf, src, dest) }
  implicit class ReprojectMultiPoint(mp: MultiPoint) { def reproject(src: CRS, dest: CRS): MultiPoint = Reproject(mp, src, dest) }
  implicit class ReprojectMultiPointFeature[D](mpf: MultiPointFeature[D]) { def reproject(src: CRS, dest: CRS): MultiPointFeature[D] = Reproject(mpf, src, dest) }
  implicit class ReprojectMutliLine(ml: MultiLine) { def reproject(src: CRS, dest: CRS): MultiLine = Reproject(ml, src, dest) }
  implicit class ReprojectMutliLineFeature[D](mlf: MultiLineFeature[D]) { def reproject(src: CRS, dest: CRS): MultiLineFeature[D] = Reproject(mlf, src, dest) }
  implicit class ReprojectMutliPolygon(mp: MultiPolygon) { def reproject(src: CRS, dest: CRS): MultiPolygon = Reproject(mp, src, dest) }
  implicit class ReprojectMutliPolygonFeature[D](mpf: MultiPolygonFeature[D]) { def reproject(src: CRS, dest: CRS): MultiPolygonFeature[D] = Reproject(mpf, src, dest) }
  implicit class ReprojectGeometryCollection(gc: GeometryCollection) { def reproject(src: CRS, dest: CRS): GeometryCollection = Reproject(gc, src, dest) }
  implicit class ReprojectGeometryCollectionFeature[D](gcf: GeometryCollectionFeature[D]) { def reproject(src: CRS, dest: CRS): GeometryCollectionFeature[D] = Reproject(gcf, src, dest) }
  implicit class ReprojectGeometry(g: Geometry) { def reproject(src: CRS, dest: CRS): Geometry = Reproject(g, src, dest) }
  implicit class ReprojectFeature[D](f: Feature[D]) { def reproject(src: CRS, dest: CRS): Feature[D] = Reproject(f, src, dest) }
}
