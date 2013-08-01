package geotrellis.io

import geotrellis._
import geotrellis.feature._

import geotrellis.data.geojson.GeoJsonWriter

case class ToGeoJson[T](feature:Op[Geometry[T]]) extends Op1(feature)({
  feature =>
    Result(GeoJsonWriter.createString(feature))
})
