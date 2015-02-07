package geotrellis.vector

import geotrellis.proj4._

package object reproject {
  object Reproject {
    def apply(t: (Double, Double), src: CRS, dest: CRS): (Double, Double) =
      apply(t, Transform(src, dest))

    def apply(t: (Double, Double), transform: Transform): (Double, Double) =
      transform(t._1, t._2)

    def apply(p: Point, src: CRS, dest: CRS): Point =
      apply(p, Transform(src, dest))

    def apply(p: Point, transform: Transform): Point =
      transform(p.x, p.y)

    def apply[D](pf:PointFeature[D], src: CRS, dest: CRS): PointFeature[D] =
      PointFeature[D](apply(pf.geom, src, dest), pf.data)

    def apply[D](pf:PointFeature[D], transform: Transform): PointFeature[D] =
      PointFeature[D](apply(pf.geom, transform), pf.data)

    def apply(l: Line, src: CRS, dest: CRS): Line =
      apply(l, Transform(src, dest))

    def apply(l: Line, transform: Transform): Line =
      Line(l.points.map { p => transform(p.x, p.y) })

    def apply[D](lf: LineFeature[D], src: CRS, dest: CRS): LineFeature[D] =
      LineFeature(apply(lf.geom, src, dest), lf.data)

    def apply[D](lf: LineFeature[D], transform: Transform): LineFeature[D] =
      LineFeature(apply(lf.geom, transform), lf.data)

    def apply(p: Polygon, src: CRS, dest: CRS): Polygon = 
      apply(p, Transform(src, dest))

    def apply(p: Polygon, transform: Transform): Polygon = {
      Polygon(
        apply(p.exterior, transform),
        p.holes.map{ apply(_, transform) }
      )
    }

    def apply(extent: Extent, src: CRS, dest: CRS): Extent = {
      val f = Transform(src, dest)
      val sw = f(extent.xmin, extent.ymin)
      val ne = f(extent.xmax, extent.ymax)
      Extent(sw._1,sw._2,ne._1,ne._2)
    }

    def apply[D](pf: PolygonFeature[D], src: CRS, dest: CRS): PolygonFeature[D] =
      PolygonFeature(apply(pf.geom, src, dest), pf.data)

    def apply[D](pf: PolygonFeature[D], transform: Transform): PolygonFeature[D] =
      PolygonFeature(apply(pf.geom, transform), pf.data)

    def apply(mp: MultiPoint, src: CRS, dest: CRS): MultiPoint =
      apply(mp, Transform(src, dest))

    def apply(mp: MultiPoint, transform: Transform): MultiPoint =
      MultiPoint(mp.points.map { p => transform(p.x, p.y) })

    def apply[D](mpf: MultiPointFeature[D], src: CRS, dest: CRS): MultiPointFeature[D] =
      MultiPointFeature(apply(mpf.geom, src, dest), mpf.data)

    def apply[D](mpf: MultiPointFeature[D], transform: Transform): MultiPointFeature[D] =
      MultiPointFeature(apply(mpf.geom, transform), mpf.data)

    def apply(ml: MultiLine, src: CRS, dest: CRS): MultiLine =
      apply(ml, Transform(src, dest))

    def apply(ml: MultiLine, transform: Transform): MultiLine =
      MultiLine(ml.lines.map(apply(_, transform)))

    def apply[D](mlf: MultiLineFeature[D], src: CRS, dest: CRS): MultiLineFeature[D] =
      MultiLineFeature(apply(mlf, src, dest), mlf.data)

    def apply[D](mlf: MultiLineFeature[D], transform: Transform): MultiLineFeature[D] =
      MultiLineFeature(apply(mlf, transform), mlf.data)

    def apply(mp: MultiPolygon, src: CRS, dest: CRS): MultiPolygon =
      apply(mp, Transform(src, dest))

    def apply(mp: MultiPolygon, transform: Transform): MultiPolygon =
      MultiPolygon(mp.polygons.map(apply(_, transform)))

    def apply[D](mpf: MultiPolygonFeature[D], src: CRS, dest: CRS): MultiPolygonFeature[D] =
      MultiPolygonFeature(apply(mpf.geom, src, dest), mpf.data)

    def apply[D](mpf: MultiPolygonFeature[D], transform: Transform): MultiPolygonFeature[D] =
      MultiPolygonFeature(apply(mpf.geom, transform: Transform), mpf.data)

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

    def apply(gc: GeometryCollection, transform: Transform): GeometryCollection =
      GeometryCollection(
        gc.points.map{ apply(_, transform) },
        gc.lines.map{ apply(_, transform) },
        gc.polygons.map{ apply(_, transform) },
        gc.multiPoints.map{ apply(_, transform) },
        gc.multiLines.map{ apply(_, transform) },
        gc.multiPolygons.map{ apply(_, transform) },
        gc.geometryCollections.map{ apply(_, transform) }
      )


    def apply[D](gcf: GeometryCollectionFeature[D], src: CRS, dest: CRS): GeometryCollectionFeature[D] =
      GeometryCollectionFeature(apply(gcf.geom, src, dest), gcf.data)

    def apply[D](gcf: GeometryCollectionFeature[D], transform: Transform): GeometryCollectionFeature[D] =
      GeometryCollectionFeature(apply(gcf.geom, transform), gcf.data)

    def apply(g: Geometry, src: CRS, dest: CRS): Geometry =
      apply(g, Transform(src, dest))

    def apply(g: Geometry, transform: Transform): Geometry =
      g match {
        case p: Point => apply(p, transform)
        case l: Line => apply(l, transform)
        case p: Polygon => apply(p, transform)
        case mp: MultiPoint => apply(mp, transform)
        case ml: MultiLine => apply(ml, transform)
        case mp: MultiPolygon => apply(mp, transform)
        case gc: GeometryCollection => apply(gc, transform)
      }


    def apply[D](f: Feature[D], src: CRS, dest: CRS): Feature[D] =
      apply(f, Transform(src, dest))

    def apply[D](f: Feature[D], transform: Transform): Feature[D] =
      f match {
        case pf: PointFeature[D] => apply(pf, transform)
        case lf: LineFeature[D] => apply(lf, transform)
        case pf: PolygonFeature[D] => apply(pf, transform)
        case mpf: MultiPointFeature[D] => apply(mpf, transform)
        case mlf: MultiLineFeature[D] => apply(mlf, transform)
        case mpf: MultiPolygonFeature[D] => apply(mpf, transform)
        case gcf: GeometryCollectionFeature[D] => apply(gcf, transform)
      }
  }

  implicit class ReprojectTuple(t: (Double,Double)) { 
    def reproject(src: CRS, dest: CRS): (Double, Double) = Reproject(t, src, dest) 
    def reproject(transform: Transform): (Double, Double) = Reproject(t, transform) 
  }

  implicit class ReprojectPoint(p: Point) { 
    def reproject(src: CRS, dest: CRS): Point = Reproject(p, src, dest) 
    def reproject(transform: Transform): Point = Reproject(p, transform) 
  }

  implicit class ReprojectPointFeature[D](pf: PointFeature[D]) { 
    def reproject(src: CRS, dest: CRS): PointFeature[D] = Reproject(pf, src, dest) 
    def reproject(transform: Transform): PointFeature[D] = Reproject(pf, transform) 
  }

  implicit class ReprojectLine(l: Line) { 
    def reproject(src: CRS, dest: CRS): Line = Reproject(l, src, dest) 
    def reproject(transform: Transform): Line = Reproject(l, transform) 
  }

  implicit class ReprojectLineFeature[D](lf: LineFeature[D]) { 
    def reproject(src: CRS, dest: CRS): LineFeature[D] = Reproject(lf, src, dest) 
    def reproject(transform: Transform): LineFeature[D] = Reproject(lf, transform) 
  }

  implicit class ReprojectPolygon(p: Polygon) { 
    def reproject(src: CRS, dest: CRS): Polygon = Reproject(p, src, dest)
    def reproject(transform: Transform): Polygon = Reproject(p, transform)
  }

  implicit class ReprojectExtent(e: Extent) { 
    def reproject(src: CRS, dest: CRS): Extent = Reproject(e, src, dest) 
    def reproject(transform: Transform): Extent = Reproject(e, transform).envelope
  }

  implicit class ReprojectPolygonFeature[D](pf: PolygonFeature[D]) { 
    def reproject(src: CRS, dest: CRS): PolygonFeature[D] = Reproject(pf, src, dest) 
    def reproject(transform: Transform): PolygonFeature[D] = Reproject(pf, transform) 
  }

  implicit class ReprojectMultiPoint(mp: MultiPoint) { 
    def reproject(src: CRS, dest: CRS): MultiPoint = Reproject(mp, src, dest) 
    def reproject(transform: Transform): MultiPoint = Reproject(mp, transform) 
  }

  implicit class ReprojectMultiPointFeature[D](mpf: MultiPointFeature[D]) { 
    def reproject(src: CRS, dest: CRS): MultiPointFeature[D] = Reproject(mpf, src, dest) 
    def reproject(transform: Transform): MultiPointFeature[D] = Reproject(mpf, transform) 
  }

  implicit class ReprojectMutliLine(ml: MultiLine) { 
    def reproject(src: CRS, dest: CRS): MultiLine = Reproject(ml, src, dest) 
    def reproject(transform: Transform): MultiLine = Reproject(ml, transform) 
  }

  implicit class ReprojectMutliLineFeature[D](mlf: MultiLineFeature[D]) { 
    def reproject(src: CRS, dest: CRS): MultiLineFeature[D] = Reproject(mlf, src, dest) 
    def reproject(transform: Transform): MultiLineFeature[D] = Reproject(mlf, transform) 
  }

  implicit class ReprojectMutliPolygon(mp: MultiPolygon) { 
    def reproject(src: CRS, dest: CRS): MultiPolygon = Reproject(mp, src, dest) 
    def reproject(transform: Transform): MultiPolygon = Reproject(mp, transform) 
  }

  implicit class ReprojectMutliPolygonFeature[D](mpf: MultiPolygonFeature[D]) { 
    def reproject(src: CRS, dest: CRS): MultiPolygonFeature[D] = Reproject(mpf, src, dest) 
    def reproject(transform: Transform): MultiPolygonFeature[D] = Reproject(mpf, transform) 
  }

  implicit class ReprojectGeometryCollection(gc: GeometryCollection) { 
    def reproject(src: CRS, dest: CRS): GeometryCollection = Reproject(gc, src, dest) 
    def reproject(transform: Transform): GeometryCollection = Reproject(gc, transform) 
  }

  implicit class ReprojectGeometryCollectionFeature[D](gcf: GeometryCollectionFeature[D]) { 
    def reproject(src: CRS, dest: CRS): GeometryCollectionFeature[D] = Reproject(gcf, src, dest) 
    def reproject(transform: Transform): GeometryCollectionFeature[D] = Reproject(gcf, transform) 
  }

  implicit class ReprojectGeometry(g: Geometry) { 
    def reproject(src: CRS, dest: CRS): Geometry = Reproject(g, src, dest) 
    def reproject(transform: Transform): Geometry = Reproject(g, transform) 
  }

  implicit class ReprojectFeature[D](f: Feature[D]) { 
    def reproject(src: CRS, dest: CRS): Feature[D] = Reproject(f, src, dest) 
    def reproject(transform: Transform): Feature[D] = Reproject(f, transform) 
  }
}
