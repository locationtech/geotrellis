package geotrellis.feature

import geotrellis.feature.srs._

package object srs {
  implicit class ExtendedGeometry(g: Geometry) {
    def transform[A <: SRS, B <: SRS](implicit megatron: Transformer[A, B]) = megatron.transform(g)
  }

  implicit val transformLatLngToWebMurcator = Transformer[LatLng, WebMercator]{ (x, y) =>
    val mx = x * SRS.originShift / 180.0
    val my1 = math.log( math.tan((90 + y) * math.Pi / 360.0 )) / (math.Pi / 180.0)
    val my = my1 * SRS.originShift / 180
    (mx, my)
  }

  implicit val transformWebMurcatorToLatLng = Transformer[WebMercator, LatLng]{ (x, y) =>
    val xlng = (x / SRS.originShift) * 180.0
    val ylat1 = (y / SRS.originShift) * 180.0
    val ylat = 180 / math.Pi * (2 * math.atan( math.exp( ylat1 * math.Pi / 180.0)) - math.Pi / 2.0)
    (xlng, ylat)
  }
}
