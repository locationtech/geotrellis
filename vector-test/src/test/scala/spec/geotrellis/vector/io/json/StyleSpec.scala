package geotrellis.vector.io.json

import org.scalatest._

import geotrellis.vector._

class StyleSpec extends FunSpec with Matchers {
  describe("Styling geometries") {
    it("should style the geometry from a user defines style and read it back in") {
      val p = """{ "type": "Polygon", "coordinates": [ [ [-76.97021484375, 40.17887331434696], [-74.02587890625, 39.842286020743394],
	           [-73.4326171875, 41.713930073371294], [-76.79443359375, 41.94314874732696], 
                   [-76.97021484375, 40.17887331434696] ] ] }""".parseGeoJson[Polygon]


      val geoJson =
        Feature(p, Style(strokeColor = "#555555", strokeWidth = 2, fillColor = "#00aa22", fillOpacity = 0.5)).toGeoJson


      val Feature(_, style) = geoJson.parseGeoJson[Feature[Polygon, Style]]
      style.strokeColor should be (Some("#555555"))
      style.strokeWidth should be (Some(2))
      style.strokeOpacity should be (None)
      style.fillColor should be (Some("#00aa22"))
      style.fillOpacity should be (Some(0.5))

      println(geoJson)
    }
  }
}
