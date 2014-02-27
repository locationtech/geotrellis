package geotrellis.io

import geotrellis._
import geotrellis.feature._
import geotrellis.logic.Collect

import geotrellis.data.geojson.GeoJsonWriter

object ToGeoJson {
  def apply[T](features:Op[Seq[Op[Geometry[T]]]])
              (implicit d:DummyImplicit):FeatureCollectionToGeoJson[T] =
    FeatureCollectionToGeoJson(Collect(features))

  def apply[T](features:Op[Seq[Geometry[T]]]):FeatureCollectionToGeoJson[T] =
    FeatureCollectionToGeoJson(features)
}

case class ToGeoJson[T](feature:Op[Geometry[T]]) extends Op1(feature)({
  feature =>
    Result(GeoJsonWriter.createString(feature))
})

case class FeatureCollectionToGeoJson[T](features:Op[Seq[Geometry[T]]]) 
extends Op1(features)({
  features =>
    Result(GeoJsonWriter.createFeatureCollectionString(features))
})
