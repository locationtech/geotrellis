package geotrellis.io

import geotrellis._
import geotrellis.feature._

import geotrellis.data.geojson.GeoJsonReader

/**
 * Load a feature from GeoJson.
 *
 * This operation loads a feature from GeoJson.  It accepts both simple
 * geometry definitions and feature definitions.  If there is a property
 * JSON clause, the feature data will be Some(JsonNode).
 */
case class LoadGeoJsonFeature(geojson:Op[String]) extends Op1(geojson)({
  (geojson) => {
    val resultOpt = GeoJsonReader.parse(geojson)
    resultOpt match {
      case None => StepError("Could not parse GeoJSON", "")
      case Some(geometryArray) => {
        val geometryCount = geometryArray.length
        if (geometryCount != 1) {
          StepError("Expected a single feature; found %d features".format(geometryCount), "")
        } else {
          Result( geometryArray(0) )
        }
      }
    } 
  }
})
