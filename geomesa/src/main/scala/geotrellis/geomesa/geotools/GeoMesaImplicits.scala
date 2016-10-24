package geotrellis.geomesa.geotools

import geotrellis.util.MethodExtensions
import geotrellis.vector.{Feature, Geometry}
import geotrellis.util.annotations.experimental

@experimental object GeoMesaImplicits extends GeoMesaImplicits

@experimental trait GeoMesaImplicits {
  implicit class withFeatureToGeoMesaSimpleFeatureMethods[G <: Geometry, T](val self: Feature[G, T])
    extends MethodExtensions[Feature[G, T]]
      with FeatureToGeoMesaSimpleFeatureMethods[G, T]
}
