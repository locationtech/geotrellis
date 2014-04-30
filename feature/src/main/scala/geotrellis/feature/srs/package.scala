package geotrellis.feature

package object srs {

  implicit class ExtendedTuple(t: (Double, Double)) {
    def transform(src: SRS, target: SRS): (Double, Double) =
      target.toCartesian(src.toEllipsoidal(t))
  }

  implicit class ExtendedPoint(p: Point) {
    def transform(src: SRS, target: SRS): Point =
      Point(target.toCartesian(src.toEllipsoidal(p.x, p.y)))
  }

  implicit class ExtendedLine(ls: Line) {
    def transform(src: SRS, target: SRS): Line =
      Line(ls.points.map{ _.transform(src, target) })
  }

  implicit class ExtendedPolygon(p: Polygon) {
    def transform(src: SRS, target: SRS): Polygon =
      Polygon(
        p.exterior.transform(src, target),
        p.holes.map{ _.transform(src, target) }.toSet
      )
  }

  implicit class ExtendedMultiPoint(mp: MultiPoint) {
    def transform(src: SRS, target: SRS): MultiPoint =
      MultiPoint(mp.points.map{ _.transform(src, target) })
  }

  implicit class ExtendedMultiLine(ml: MultiLine) {
    def transform(src: SRS, target: SRS): MultiLine =
      MultiLine(ml.lines.map{ _.transform(src, target) })
  }

  implicit class ExtendedMultiPolygon(mp: MultiPolygon) {
    def transform(src: SRS, target: SRS): MultiPolygon =
      MultiPolygon(mp.polygons.map{ _.transform(src, target) })
  }

  implicit class ExtendedGeometryCollection(gc: GeometryCollection) {
    def transform(src: SRS, target: SRS): GeometryCollection =
       GeometryCollection(
        gc.points.map{ _.transform(src, target) },
        gc.lines.map{ _.transform(src, target) },
        gc.polygons.map{ _.transform(src, target) },
        gc.multiPoints.map{ _.transform(src, target) },
        gc.multiLines.map{ _.transform(src, target) },
        gc.multiPolygons.map{ _.transform(src, target) },
        gc.geometryCollections.map{ _.transform(src, target) }
      )
  }

  implicit class ExtendedGeometry(g: Geometry) {
    def transform(src: SRS, target: SRS): Geometry = {
      g match {
        case g: Point => g.transform(src, target)
        case g: MultiPoint => g.transform(src, target)
        case g: Line => g.transform(src, target)
        case g: MultiLine => g.transform(src, target)
        case g: Polygon => g.transform(src, target)
        case g: MultiPolygon => g.transform(src, target)
      }
    }
  }

  val `web-mercator` = new WebMercator
  val `lat-lng` = new LatLng

//  implicit val transformLatLngToWebMurcator = Transformer[LatLng, WebMercator]{ (x, y) =>
//    val mx = x * SRS.originShift / 180.0
//    val my1 = math.log( math.tan((90 + y) * math.Pi / 360.0 )) / (math.Pi / 180.0)
//    val my = my1 * SRS.originShift / 180
//    (mx, my)
//  }
//
//  implicit val transformWebMurcatorToLatLng = Transformer[WebMercator, LatLng]{ (x, y) =>
//    val xlng = (x / SRS.originShift) * 180.0
//    val ylat1 = (y / SRS.originShift) * 180.0
//    val ylat = 180 / math.Pi * (2 * math.atan( math.exp( ylat1 * math.Pi / 180.0)) - math.Pi / 2.0)
//    (xlng, ylat)
//  }
}
